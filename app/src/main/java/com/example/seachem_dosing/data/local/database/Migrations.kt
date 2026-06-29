package com.example.seachem_dosing.data.local.database

import android.database.Cursor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.LegacyParameterMapping
import com.example.seachem_dosing.domain.history.LegacyUnitMapper
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.ParameterValidationStatus
import com.example.seachem_dosing.domain.history.PrecisionStatus
import com.example.seachem_dosing.domain.history.UnitCode

private const val MIGRATION_SOURCE_MODULE = "legacy_migration"
private const val SCHEMA_VERSION_V2 = 2

/**
 * Non-destructive v1 → v2 history migration (ADR-011 §5/§11). Creates the v2 schema, converts any
 * v1 rows under the accepted defensive legacy semantics (the v1 writer was orphaned, so rows are not
 * expected but are handled correctly), validates counts + foreign keys, then drops the v1 tables.
 * Any mismatch throws so Room rolls the migration transaction back.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        createV2Schema(db)

        var events = 0L
        var doseDetails = 0L
        var paramDetails = 0L

        db.query(
            "SELECT id, profile_id, timestamp, product, amount, unit, volume_litres_at_dose, administered, notes FROM dosing_log",
        ).use { c ->
            while (c.moveToNext()) {
                migrateDoseRow(db, c)
                events++; doseDetails++
            }
        }

        db.query(
            "SELECT id, profile_id, timestamp, ammonia, nitrite, nitrate, gh, kh, ph, temperature, salinity, " +
                "alkalinity, calcium, magnesium, phosphate, dissolved_oxygen, potassium, iron, strontium, iodide, " +
                "volume_litres, notes FROM parameter_log",
        ).use { c ->
            while (c.moveToNext()) {
                val (e, d) = migrateParameterRow(db, c)
                events += e; paramDetails += d
            }
        }

        validateCounts(db, events, doseDetails, paramDetails)
        db.query("PRAGMA foreign_key_check").use {
            check(!it.moveToNext()) { "Migration(1,2): foreign_key_check reported a violation" }
        }

        db.execSQL("DROP TABLE `dosing_log`")
        db.execSQL("DROP TABLE `parameter_log`")
    }
}

private fun migrateDoseRow(db: SupportSQLiteDatabase, c: Cursor) {
    val id = c.getLong(0)
    val profile = c.getString(1)
    val ts = c.getLong(2)
    val product = c.getString(3)
    val amount = c.getDouble(4)
    val unit = c.getString(5)
    val volume = c.getDouble(6)
    val administered = c.getInt(7) != 0
    val notes = if (c.isNull(8)) null else c.getString(8)

    val eventId = "legacy-v1:dosing_log:$id"
    val type = if (administered) HistoryEventType.LEGACY_DOSE_ADMINISTERED else HistoryEventType.LEGACY_DOSE_CALCULATION
    val mapping = LegacyUnitMapper.map(unit)
    val amountStr = StoredDecimal.fromLegacyBinary64(amount).canonicalValue
    val volumeStr = StoredDecimal.fromLegacyBinary64(volume).canonicalValue

    insertEvent(db, eventId, type, profile, ts, "migration:v1-to-v2:dosing_log:$id", notes)
    db.execSQL(
        "INSERT INTO `dose_event_detail` (`event_id`,`legacy_product_label`,`tank_volume_decimal`," +
            "`tank_volume_unit_code`,`calculated_amount_decimal`,`calculated_amount_unit_code`," +
            "`calculated_measure_definition_id`,`administered_amount_decimal`,`administered_amount_unit_code`," +
            "`administered_measure_definition_id`,`legacy_original_unit_text`,`user_modified_amount`) " +
            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
        arrayOf<Any?>(
            eventId, product, volumeStr, UnitCode.LITER.storageCode,
            amountStr, mapping.unitCode.storageCode, mapping.measureDefinitionId,
            if (administered) amountStr else null,
            if (administered) mapping.unitCode.storageCode else null,
            if (administered) mapping.measureDefinitionId else null,
            mapping.originalText, 0,
        ),
    )
}

private val PARAMETER_COLUMNS = listOf(
    3 to ParameterType.AMMONIA, 4 to ParameterType.NITRITE, 5 to ParameterType.NITRATE,
    6 to ParameterType.GH, 7 to ParameterType.KH, 8 to ParameterType.PH, 9 to ParameterType.TEMPERATURE,
    10 to ParameterType.SALINITY, 11 to ParameterType.ALKALINITY, 12 to ParameterType.CALCIUM,
    13 to ParameterType.MAGNESIUM, 14 to ParameterType.PHOSPHATE, 15 to ParameterType.DISSOLVED_OXYGEN,
    16 to ParameterType.POTASSIUM, 17 to ParameterType.IRON, 18 to ParameterType.STRONTIUM,
    19 to ParameterType.IODIDE,
)

/** @return (events created, parameter details created) for one v1 parameter_log row. */
private fun migrateParameterRow(db: SupportSQLiteDatabase, c: Cursor): Pair<Long, Long> {
    val id = c.getLong(0)
    val profile = c.getString(1)
    val ts = c.getLong(2)
    val volumeStr = StoredDecimal.fromLegacyBinary64(c.getDouble(20)).canonicalValue
    val notes = if (c.isNull(21)) null else c.getString(21)

    var details = 0L
    for ((idx, type) in PARAMETER_COLUMNS) {
        if (c.isNull(idx)) continue
        val eventId = "legacy-v1:parameter_log:$id:${type.storageCode}"
        insertEvent(
            db, eventId, HistoryEventType.LEGACY_PARAMETER_RECORD, profile, ts,
            "migration:v1-to-v2:parameter_log:$id:${type.storageCode}", notes,
        )
        db.execSQL(
            "INSERT INTO `parameter_event_detail` (`event_id`,`parameter_type_code`,`measured_value_decimal`," +
                "`measured_unit_code`,`tank_volume_decimal`,`tank_volume_unit_code`,`validation_status_code`) " +
                "VALUES (?,?,?,?,?,?,?)",
            arrayOf<Any?>(
                eventId, type.storageCode, StoredDecimal.fromLegacyBinary64(c.getDouble(idx)).canonicalValue,
                LegacyParameterMapping.unitFor(type).storageCode, volumeStr, UnitCode.LITER.storageCode,
                ParameterValidationStatus.UNVALIDATED.storageCode,
            ),
        )
        details++
    }
    if (details == 0L) {
        // all-null row → evidence-preserving empty snapshot (no detail)
        insertEvent(
            db, "legacy-v1:parameter_log:$id:empty", HistoryEventType.LEGACY_PARAMETER_SNAPSHOT_EMPTY,
            profile, ts, "migration:v1-to-v2:parameter_log:$id:empty", notes,
        )
        return 1L to 0L
    }
    return details to details
}

private fun insertEvent(
    db: SupportSQLiteDatabase,
    eventId: String,
    type: HistoryEventType,
    profileId: String,
    timestamp: Long,
    idempotencyKey: String,
    notes: String?,
) {
    db.execSQL(
        "INSERT INTO `history_event` (`event_id`,`event_type_code`,`aquarium_profile_id`," +
            "`occurred_at_epoch_millis`,`created_at_epoch_millis`,`source_module_code`,`idempotency_key`," +
            "`schema_version`,`precision_status_code`,`notes`) VALUES (?,?,?,?,?,?,?,?,?,?)",
        arrayOf<Any?>(
            eventId, type.storageCode, profileId, timestamp, timestamp, MIGRATION_SOURCE_MODULE,
            idempotencyKey, SCHEMA_VERSION_V2, PrecisionStatus.LEGACY_BINARY64_APPROXIMATION.storageCode, notes,
        ),
    )
}

private fun validateCounts(db: SupportSQLiteDatabase, events: Long, doseDetails: Long, paramDetails: Long) {
    check(count(db, "history_event") == events) { "Migration(1,2): history_event count mismatch" }
    check(count(db, "dose_event_detail") == doseDetails) { "Migration(1,2): dose_event_detail count mismatch" }
    check(count(db, "parameter_event_detail") == paramDetails) { "Migration(1,2): parameter_event_detail count mismatch" }
}

private fun count(db: SupportSQLiteDatabase, table: String): Long =
    db.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getLong(0) }

/**
 * Creates the v2 tables + indexes. DDL is kept byte-identical to Room's exported `2.json`
 * `createSql` so `MigrationTestHelper.runMigrationsAndValidate` passes; verified against the export.
 */
private fun createV2Schema(db: SupportSQLiteDatabase) {
    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `history_event` (`event_id` TEXT NOT NULL, `event_type_code` TEXT NOT NULL, " +
            "`aquarium_profile_id` TEXT NOT NULL, `occurred_at_epoch_millis` INTEGER NOT NULL, " +
            "`created_at_epoch_millis` INTEGER NOT NULL, `source_module_code` TEXT NOT NULL, `app_version` TEXT, " +
            "`engine_version` TEXT, `idempotency_key` TEXT NOT NULL, `schema_version` INTEGER NOT NULL, " +
            "`precision_status_code` TEXT NOT NULL, `notes` TEXT, `supersedes_event_id` TEXT, " +
            "`voids_event_id` TEXT, `correction_reason` TEXT, PRIMARY KEY(`event_id`), " +
            "FOREIGN KEY(`supersedes_event_id`) REFERENCES `history_event`(`event_id`) ON UPDATE NO ACTION ON DELETE RESTRICT, " +
            "FOREIGN KEY(`voids_event_id`) REFERENCES `history_event`(`event_id`) ON UPDATE NO ACTION ON DELETE RESTRICT)",
    )
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_history_event_idempotency_key` ON `history_event` (`idempotency_key`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_event_aquarium_profile_id_occurred_at_epoch_millis` ON `history_event` (`aquarium_profile_id`, `occurred_at_epoch_millis`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_event_event_type_code` ON `history_event` (`event_type_code`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_event_supersedes_event_id` ON `history_event` (`supersedes_event_id`)")
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_event_voids_event_id` ON `history_event` (`voids_event_id`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `dose_event_detail` (`event_id` TEXT NOT NULL, `product_id` TEXT, " +
            "`legacy_product_label` TEXT, `product_variant_id` TEXT, `formula_rule_id` TEXT, `evidence_source_id` TEXT, " +
            "`route_code` TEXT, `concentration_decimal` TEXT, `concentration_unit_code` TEXT, " +
            "`tank_volume_decimal` TEXT NOT NULL, `tank_volume_unit_code` TEXT NOT NULL, " +
            "`calculated_amount_decimal` TEXT, `calculated_amount_unit_code` TEXT, " +
            "`calculated_measure_definition_id` TEXT, `administered_amount_decimal` TEXT, " +
            "`administered_amount_unit_code` TEXT, `administered_measure_definition_id` TEXT, " +
            "`legacy_original_unit_text` TEXT, `rounding_mode_code` TEXT, `rounding_scale` INTEGER, " +
            "`user_modified_amount` INTEGER NOT NULL, `warnings_acknowledged` TEXT, PRIMARY KEY(`event_id`), " +
            "FOREIGN KEY(`event_id`) REFERENCES `history_event`(`event_id`) ON UPDATE NO ACTION ON DELETE RESTRICT)",
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_dose_event_detail_product_id` ON `dose_event_detail` (`product_id`)")

    db.execSQL(
        "CREATE TABLE IF NOT EXISTS `parameter_event_detail` (`event_id` TEXT NOT NULL, " +
            "`parameter_type_code` TEXT NOT NULL, `measured_value_decimal` TEXT NOT NULL, " +
            "`measured_unit_code` TEXT NOT NULL, `tank_volume_decimal` TEXT, `tank_volume_unit_code` TEXT, " +
            "`test_method` TEXT, `source_device_or_kit` TEXT, `validation_status_code` TEXT NOT NULL, " +
            "PRIMARY KEY(`event_id`), " +
            "FOREIGN KEY(`event_id`) REFERENCES `history_event`(`event_id`) ON UPDATE NO ACTION ON DELETE RESTRICT)",
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_parameter_event_detail_parameter_type_code` ON `parameter_event_detail` (`parameter_type_code`)")
}
