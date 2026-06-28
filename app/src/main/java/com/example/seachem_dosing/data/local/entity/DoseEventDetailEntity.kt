package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Dose-event detail (ADR-011 §1), 1:1 with its [HistoryEventEntity] via [eventId] (PK + FK,
 * `ON DELETE RESTRICT`). `*_decimal` columns hold canonical [com.example.seachem_dosing.core
 * .numerics.StoredDecimal] strings (never REAL/Double); `*_unit_code` columns hold [UnitCode]
 * storage codes. Calculated vs administered amounts are kept separate (never overwritten).
 *
 * Scoop/calibrated-spoon administration: when [administeredAmountUnitCode] is a calibration unit
 * (`scoop_mfr` / `spoon_user`), the companion fields capture the scoop-definition id or the
 * user's calibration volume + unit — a unit label alone is not a measurement (ADR-011 §3).
 */
@Entity(
    tableName = "dose_event_detail",
    foreignKeys = [
        ForeignKey(
            entity = HistoryEventEntity::class,
            parentColumns = ["event_id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["product_id"])],
)
data class DoseEventDetailEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_variant_id") val productVariantId: String? = null,
    @ColumnInfo(name = "formula_rule_id") val formulaRuleId: String? = null,
    @ColumnInfo(name = "evidence_source_id") val evidenceSourceId: String? = null,
    @ColumnInfo(name = "route_code") val routeCode: String? = null,
    @ColumnInfo(name = "concentration_decimal") val concentrationDecimal: String? = null,
    @ColumnInfo(name = "concentration_unit_code") val concentrationUnitCode: String? = null,
    @ColumnInfo(name = "tank_volume_decimal") val tankVolumeDecimal: String,
    @ColumnInfo(name = "tank_volume_unit_code") val tankVolumeUnitCode: String,
    @ColumnInfo(name = "calculated_amount_decimal") val calculatedAmountDecimal: String? = null,
    @ColumnInfo(name = "calculated_amount_unit_code") val calculatedAmountUnitCode: String? = null,
    @ColumnInfo(name = "administered_amount_decimal") val administeredAmountDecimal: String,
    @ColumnInfo(name = "administered_amount_unit_code") val administeredAmountUnitCode: String,
    @ColumnInfo(name = "rounding_mode_code") val roundingModeCode: String? = null,
    @ColumnInfo(name = "rounding_scale") val roundingScale: Int? = null,
    @ColumnInfo(name = "user_modified_amount") val userModifiedAmount: Boolean,
    @ColumnInfo(name = "warnings_acknowledged") val warningsAcknowledged: String? = null,
    @ColumnInfo(name = "administered_scoop_definition_id") val administeredScoopDefinitionId: String? = null,
    @ColumnInfo(name = "administered_calibrated_volume_decimal") val administeredCalibratedVolumeDecimal: String? = null,
    @ColumnInfo(name = "administered_calibrated_volume_unit_code") val administeredCalibratedVolumeUnitCode: String? = null,
)
