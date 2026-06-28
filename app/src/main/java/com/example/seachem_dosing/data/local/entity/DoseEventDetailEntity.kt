package com.example.seachem_dosing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Dose-event detail (ADR-011 §1), 1:1 with its [HistoryEventEntity] via [eventId] (PK + FK,
 * `ON DELETE RESTRICT`). `*_decimal` columns hold canonical
 * [com.example.seachem_dosing.core.numerics.StoredDecimal] strings (never REAL/Double);
 * `*_unit_code` hold `UnitCode` storage codes. Calculated vs administered amounts are separate
 * (never overwritten).
 *
 * Nullability supports honest legacy migration; the repository validator enforces stricter
 * per-event-type requirements for new exact records:
 *  - `LEGACY_DOSE_CALCULATION`: calculated_* set, administered_* null.
 *  - `LEGACY_DOSE_ADMINISTERED`: calculated_* and administered_* set (same legacy value), never the
 *    modern DOSE_ADMINISTERED type.
 *  - new `DOSE_ADMINISTERED`: administered_* required, product_id required.
 *
 * Engine-defined / calibrated units (`requiresMeasureDefinition`) carry a
 * `*_measure_definition_id` referencing an immutable measure definition. Exact v1 product/unit text
 * is preserved in [legacyProductLabel]/[legacyOriginalUnitText] (machine-significant, not notes).
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
    @ColumnInfo(name = "product_id") val productId: String? = null,
    @ColumnInfo(name = "legacy_product_label") val legacyProductLabel: String? = null,
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
    @ColumnInfo(name = "calculated_measure_definition_id") val calculatedMeasureDefinitionId: String? = null,
    @ColumnInfo(name = "administered_amount_decimal") val administeredAmountDecimal: String? = null,
    @ColumnInfo(name = "administered_amount_unit_code") val administeredAmountUnitCode: String? = null,
    @ColumnInfo(name = "administered_measure_definition_id") val administeredMeasureDefinitionId: String? = null,
    @ColumnInfo(name = "legacy_original_unit_text") val legacyOriginalUnitText: String? = null,
    @ColumnInfo(name = "rounding_mode_code") val roundingModeCode: String? = null,
    @ColumnInfo(name = "rounding_scale") val roundingScale: Int? = null,
    @ColumnInfo(name = "user_modified_amount") val userModifiedAmount: Boolean,
    @ColumnInfo(name = "warnings_acknowledged") val warningsAcknowledged: String? = null,
)
