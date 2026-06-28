package com.example.seachem_dosing.domain.history

import com.example.seachem_dosing.core.numerics.StoredDecimal

enum class MeasureCategory { MASS, VOLUME }

/** Where a measure definition comes from. These legacy ones are the app's own historical engine. */
enum class MeasureProvenance { LEGACY_APPLICATION_ENGINE }

/**
 * An immutable, versioned definition of a non-physical measure (scoop / capful / calibrated spoon /
 * legacy engine spoon). A definition pins the canonical quantity + unit so the same id always means
 * the same amount (ADR-011). [manufacturerVerified] = false marks an app-engine assumption, NOT a
 * manufacturer claim.
 */
data class MeasureDefinition(
    val id: String,
    val category: MeasureCategory,
    val canonicalValue: StoredDecimal,
    val canonicalUnit: UnitCode,
    val provenance: MeasureProvenance,
    val manufacturerVerified: Boolean,
    val sourceReference: String,
    val productOrRuleId: String? = null,
    val version: Int = 1,
)

/**
 * Registry of legacy engine-defined measures (ADR-011). Only definitions PROVEN by historical
 * engine code are present:
 *  - [CAPFUL_5ML] — `SeachemCalculations.kt:208` `ml.divide(5)` ⇒ 1 capful = 5 mL.
 *  - [TABLESPOON_16G] — `SeachemCalculations.kt:160` `grams.divide(16)` ⇒ 1 tablespoon = 16 g.
 *
 * No product-specific teaspoon definition exists: the v1 history writer was orphaned, so
 * `dosing_log.product` has no label contract and no data — legacy `tsp` ⇒ `LEGACY_UNSPECIFIED`.
 */
object LegacyMeasureDefinitions {
    const val CAPFUL_5ML = "legacy-engine:capful:5ml"
    const val TABLESPOON_16G = "legacy-engine:tablespoon:16g"

    private val byId: Map<String, MeasureDefinition> = listOf(
        MeasureDefinition(
            id = CAPFUL_5ML,
            category = MeasureCategory.VOLUME,
            canonicalValue = StoredDecimal.parseNewValue("5"),
            canonicalUnit = UnitCode.MILLILITER,
            provenance = MeasureProvenance.LEGACY_APPLICATION_ENGINE,
            manufacturerVerified = false,
            sourceReference = "SeachemCalculations.kt:208 ml.divide(BigDecimal(\"5\"))",
        ),
        MeasureDefinition(
            id = TABLESPOON_16G,
            category = MeasureCategory.MASS,
            canonicalValue = StoredDecimal.parseNewValue("16"),
            canonicalUnit = UnitCode.GRAM,
            provenance = MeasureProvenance.LEGACY_APPLICATION_ENGINE,
            manufacturerVerified = false,
            sourceReference = "SeachemCalculations.kt:160 grams.divide(BigDecimal(\"16\"))",
        ),
    ).associateBy { it.id }

    init { require(byId.size == 2) { "duplicate legacy measure-definition id" } }

    fun get(id: String): MeasureDefinition? = byId[id]
    fun all(): List<MeasureDefinition> = byId.values.toList()
}
