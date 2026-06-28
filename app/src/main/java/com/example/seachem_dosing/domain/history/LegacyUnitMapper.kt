package com.example.seachem_dosing.domain.history

/** A v1 unit string mapped to a v2 [UnitCode] + optional immutable measure-definition id. */
data class LegacyUnitMapping(
    val unitCode: UnitCode,
    val measureDefinitionId: String?,
    val originalText: String,
)

/**
 * Maps a v1 `dosing_log.unit` string to v2 (ADR-011). Proportionate + honest: the v1 history writer
 * was orphaned (no product-label contract, no data), so legacy `tsp` ⇒ [UnitCode.LEGACY_UNSPECIFIED]
 * with raw text preserved and NO mass/volume conversion. `g`/`mL` are real physical units; `caps`
 * and `tbsp` are engine-defined measures referencing [LegacyMeasureDefinitions]. Never maps
 * `tsp`/`tbsp` to a US/metric volume spoon.
 */
object LegacyUnitMapper {
    fun map(v1Unit: String): LegacyUnitMapping = when (v1Unit) {
        "g" -> LegacyUnitMapping(UnitCode.GRAM, null, v1Unit)
        "mL" -> LegacyUnitMapping(UnitCode.MILLILITER, null, v1Unit)
        "caps" -> LegacyUnitMapping(UnitCode.LEGACY_ENGINE_VOLUME_MEASURE, LegacyMeasureDefinitions.CAPFUL_5ML, v1Unit)
        "tbsp" -> LegacyUnitMapping(UnitCode.LEGACY_ENGINE_MASS_MEASURE, LegacyMeasureDefinitions.TABLESPOON_16G, v1Unit)
        else -> LegacyUnitMapping(UnitCode.LEGACY_UNSPECIFIED, null, v1Unit) // "tsp" and any unknown string
    }
}
