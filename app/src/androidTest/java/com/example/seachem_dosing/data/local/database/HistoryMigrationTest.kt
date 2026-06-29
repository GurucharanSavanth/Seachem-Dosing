package com.example.seachem_dosing.data.local.database

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** ADR-011 §11 — non-destructive Migration(1,2) verified against exported schemas on-device. */
@RunWith(AndroidJUnit4::class)
class HistoryMigrationTest {

    private companion object { const val DB = "history-migration-test" }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private fun v1() = helper.createDatabase(DB, 1)
    private fun migrate(): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(DB, 2, true, MIGRATION_1_2)

    private fun scalar(db: SupportSQLiteDatabase, sql: String): String? =
        db.query(sql).use { c: Cursor -> if (!c.moveToFirst() || c.isNull(0)) null else c.getString(0) }

    private fun count(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { it.moveToFirst(); it.getLong(0) }

    @Test fun emptyV1_migratesAndValidatesV2Schema_andDropsV1() {
        v1().close()
        migrate().close() // runMigrationsAndValidate(validateDroppedTables=true) asserts v1 tables gone + 2.json match
    }

    @Test fun doseRows_administeredFlag_drivesLegacyEventType_andUnitMapping() {
        v1().apply {
            execSQL("INSERT INTO dosing_log (profile_id,timestamp,product,amount,unit,volume_litres_at_dose,administered,notes) VALUES ('fw',1000,'Prime',5.0,'mL',100.0,1,NULL)")
            execSQL("INSERT INTO dosing_log (profile_id,timestamp,product,amount,unit,volume_litres_at_dose,administered,notes) VALUES ('fw',2000,'Equilibrium',7.0,'tsp',80.0,0,'n')")
            close()
        }
        migrate().use { db ->
            assertEquals("legacy_dose_administered", scalar(db, "SELECT event_type_code FROM history_event WHERE event_id='legacy-v1:dosing_log:1'"))
            assertEquals("legacy_dose_calculation", scalar(db, "SELECT event_type_code FROM history_event WHERE event_id='legacy-v1:dosing_log:2'"))
            assertEquals("legacy_binary64_approx", scalar(db, "SELECT precision_status_code FROM history_event WHERE event_id='legacy-v1:dosing_log:1'"))
            // mL -> ml; administered set on the administered row
            assertEquals("5.0", scalar(db, "SELECT administered_amount_decimal FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:1'"))
            assertEquals("ml", scalar(db, "SELECT administered_amount_unit_code FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:1'"))
            assertEquals("Prime", scalar(db, "SELECT legacy_product_label FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:1'"))
            // tsp -> LEGACY_UNSPECIFIED, raw preserved, administered null (calculation only)
            assertEquals("legacy_unspecified", scalar(db, "SELECT calculated_amount_unit_code FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:2'"))
            assertEquals("tsp", scalar(db, "SELECT legacy_original_unit_text FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:2'"))
            assertNull(scalar(db, "SELECT administered_amount_decimal FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:2'"))
        }
    }

    @Test fun caps_and_tbsp_mapToEngineMeasures() {
        v1().apply {
            execSQL("INSERT INTO dosing_log (profile_id,timestamp,product,amount,unit,volume_litres_at_dose,administered,notes) VALUES ('fw',1,'P',2.0,'caps',40.0,1,NULL)")
            execSQL("INSERT INTO dosing_log (profile_id,timestamp,product,amount,unit,volume_litres_at_dose,administered,notes) VALUES ('fw',2,'Q',3.0,'tbsp',40.0,1,NULL)")
            close()
        }
        migrate().use { db ->
            assertEquals("legacy_engine_volume", scalar(db, "SELECT administered_amount_unit_code FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:1'"))
            assertEquals("legacy-engine:capful:5ml", scalar(db, "SELECT administered_measure_definition_id FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:1'"))
            assertEquals("legacy_engine_mass", scalar(db, "SELECT administered_amount_unit_code FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:2'"))
            assertEquals("legacy-engine:tablespoon:16g", scalar(db, "SELECT administered_measure_definition_id FROM dose_event_detail WHERE event_id='legacy-v1:dosing_log:2'"))
        }
    }

    @Test fun parameterRow_fansOutPerNonNullColumn_withTankVolumeAndUnits() {
        v1().apply {
            execSQL("INSERT INTO parameter_log (profile_id,timestamp,ammonia,ph,temperature,volume_litres,notes) VALUES ('fw',1000,0.25,7.4,26.0,100.0,'n')")
            close()
        }
        migrate().use { db ->
            assertEquals(3L, count(db, "SELECT COUNT(*) FROM history_event"))
            assertEquals(3L, count(db, "SELECT COUNT(*) FROM parameter_event_detail"))
            assertEquals("ph_value", scalar(db, "SELECT measured_unit_code FROM parameter_event_detail WHERE event_id='legacy-v1:parameter_log:1:ph'"))
            assertEquals("celsius", scalar(db, "SELECT measured_unit_code FROM parameter_event_detail WHERE event_id='legacy-v1:parameter_log:1:temperature'"))
            assertEquals("ppm_mg_per_l", scalar(db, "SELECT measured_unit_code FROM parameter_event_detail WHERE event_id='legacy-v1:parameter_log:1:ammonia'"))
            assertEquals("100.0", scalar(db, "SELECT tank_volume_decimal FROM parameter_event_detail WHERE event_id='legacy-v1:parameter_log:1:ph'"))
            assertEquals("l", scalar(db, "SELECT tank_volume_unit_code FROM parameter_event_detail WHERE event_id='legacy-v1:parameter_log:1:ph'"))
        }
    }

    @Test fun all17ParameterColumns_eachProduceAnEvent() {
        v1().apply {
            execSQL(
                "INSERT INTO parameter_log (profile_id,timestamp,ammonia,nitrite,nitrate,gh,kh,ph,temperature,salinity," +
                    "alkalinity,calcium,magnesium,phosphate,dissolved_oxygen,potassium,iron,strontium,iodide,volume_litres,notes) " +
                    "VALUES ('sw',1000,0.1,0.1,5.0,8.0,6.0,8.2,26.0,35.0,8.0,420.0,1300.0,0.05,7.0,15.0,0.1,8.0,0.06,200.0,NULL)",
            )
            close()
        }
        migrate().use { db ->
            assertEquals(17L, count(db, "SELECT COUNT(*) FROM parameter_event_detail"))
            assertEquals(17L, count(db, "SELECT COUNT(*) FROM history_event"))
        }
    }

    @Test fun allNullParameterRow_becomesEmptySnapshot_noDetail() {
        v1().apply {
            execSQL("INSERT INTO parameter_log (profile_id,timestamp,volume_litres,notes) VALUES ('fw',1000,50.0,'empty')")
            close()
        }
        migrate().use { db ->
            assertEquals(1L, count(db, "SELECT COUNT(*) FROM history_event"))
            assertEquals(0L, count(db, "SELECT COUNT(*) FROM parameter_event_detail"))
            assertEquals("legacy_parameter_snapshot_empty", scalar(db, "SELECT event_type_code FROM history_event WHERE event_id='legacy-v1:parameter_log:1:empty'"))
        }
    }
}
