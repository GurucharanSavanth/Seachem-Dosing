package com.example.seachem_dosing.domain.medication

import com.example.seachem_dosing.core.result.CalcResult
import java.math.BigDecimal

/**
 * Evidence-grounded medication decision support — NOT diagnosis (SPEC §G).
 * Hard safety gates from §V4–V6: refuses (typed) rather than guessing.
 *
 * Order matters: gather required inputs FIRST (NeedsMoreInput) so we never
 * block or advise on partial context.
 */
object MedicationSafetyEngine {

    data class MedAdvice(
        val product: MedProduct,
        val doseRule: String,
        val warnings: List<String>,
    )

    data class TankContext(
        val waterType: WaterType? = null,
        val volumeLitres: BigDecimal? = null,
        val hasInvertsOrCorals: Boolean? = null,
        val filtrationAcknowledged: Boolean = false,
        val speciesConfirmed: Boolean = false,
        val priorActives: List<String> = emptyList(),
    )

    private const val ESCALATION = "When in doubt, confirm with the manufacturer label or a qualified aquatic vet."

    fun assess(product: MedProduct, ctx: TankContext): CalcResult<MedAdvice> {
        // §V5 — high-risk actives require full context before any dose.
        if (product.highRisk) {
            val missing = buildList {
                if (ctx.waterType == null) add("water type")
                if (ctx.volumeLitres == null || ctx.volumeLitres.signum() <= 0) add("tank volume")
                if (ctx.hasInvertsOrCorals == null) add("invertebrates/corals present?")
                if (!ctx.filtrationAcknowledged) add("carbon/UV/filtration acknowledged")
                if (!ctx.speciesConfirmed) add("species confirmed")
            }
            if (missing.isNotEmpty()) {
                return CalcResult.NeedsMoreInput(missing, "${product.name} is high-risk — confirm these before dosing.")
            }
        }

        // §V4 — water-type compatibility is never assumed.
        val wt = ctx.waterType
        if (wt != null && wt !in product.waterTypes) {
            return CalcResult.UnsafeBlocked(
                reason = "${product.name} is not verified for ${wt.name.lowercase()} use.",
                evidence = "Label water types: ${product.waterTypes.joinToString { it.name.lowercase() }}",
                escalation = ESCALATION,
            )
        }

        // §V5 — invertebrates/corals + an invert-sensitive or non-reef-safe med.
        if (ctx.hasInvertsOrCorals == true && (product.removeInverts || product.reefSafe == ReefSafety.NOT_SAFE)) {
            return CalcResult.UnsafeBlocked(
                reason = "${product.name} is unsafe with invertebrates/corals present — remove them or treat in a separate hospital tank.",
                evidence = if (product.reefSafe == ReefSafety.NOT_SAFE) "Manufacturer: not reef-safe." else "Manufacturer: remove invertebrates during treatment.",
                escalation = ESCALATION,
            )
        }

        // Duplicate active ingredient with a prior/concurrent treatment.
        val overlap = product.actives.map { it.lowercase() }.intersect(ctx.priorActives.map { it.lowercase() }.toSet())
        if (overlap.isNotEmpty()) {
            return CalcResult.UnsafeBlocked(
                reason = "Duplicate active ingredient (${overlap.joinToString()}) with a prior treatment — risk of overdose.",
                escalation = ESCALATION,
            )
        }

        // Cleared — surface the mandatory label instructions (§V6).
        val warnings = buildList {
            if (product.removeChemFiltration) add("Remove activated carbon / chemical filtration and turn off UV/ozone during dosing.")
            if (product.removeInverts) add("Ensure no invertebrates/corals are in the tank.")
            if (!product.doseVerified) add("Dose not yet confirmed from the manufacturer page — verify the label before dosing.")
            if (product.evidence == Evidence.SECONDARY) add("Sourced from a secondary reference; confirm against the manufacturer label.")
            add(ESCALATION)
        }
        return CalcResult.Success(MedAdvice(product, product.doseRule, warnings))
    }
}
