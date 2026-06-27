package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import com.example.seachem_dosing.core.result.failureOrNull
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * DIY fertilizer chemistry (SPEC §V2, §V7).
 *
 * Molar masses + nutrient mass-fractions are from DEEP_RESEARCH_REPORT WS3,
 * computed from IUPAC standard atomic weights (arithmetic shown in the report).
 * These are stable reference constants — unlike the medication *product*
 * catalog, which lives in versioned JSON seed (Phase 6/9).
 *
 * Core identity: `ppm(mg/L) = dose_g × nutrient_fraction × 1000 / volume_L`.
 */
object FertilizerChemistryEngine {

    private val MC = MathContext(12, RoundingMode.HALF_UP)
    private val THOUSAND = BigDecimal("1000")
    private fun bd(s: String) = BigDecimal(s)

    /** A nutrient and the fraction (0..1) of the compound's mass it contributes. */
    data class Nutrient(val symbol: String, val massFraction: BigDecimal)

    enum class Category { MACRO, MICRO, GH_KH, REEF }

    /** A DIY compound. [molarMass] g/mol; [nutrients] verified WS3 mass fractions. */
    data class Compound(
        val id: String,
        val formula: String,
        val molarMass: BigDecimal,
        val nutrients: List<Nutrient>,
        val category: Category,
    )

    data class PpmResult(val nutrient: String, val ppm: BigDecimal)

    /** Verified WS3 catalog (fractions = element mass / molar mass). */
    val CATALOG: Map<String, Compound> = listOf(
        Compound("KNO3", "KNO3", bd("101.102"),
            listOf(Nutrient("NO3-N", bd("0.13853")), Nutrient("K", bd("0.38672"))), Category.MACRO),
        Compound("KH2PO4", "KH2PO4", bd("136.084"),
            listOf(Nutrient("P", bd("0.22761")), Nutrient("K", bd("0.28730"))), Category.MACRO),
        Compound("K2SO4", "K2SO4", bd("174.252"),
            listOf(Nutrient("K", bd("0.44875")), Nutrient("S", bd("0.18399"))), Category.MACRO),
        Compound("MgSO4_7H2O", "MgSO4·7H2O", bd("246.466"),
            listOf(Nutrient("Mg", bd("0.09862")), Nutrient("S", bd("0.13008"))), Category.GH_KH),
        Compound("CaSO4_2H2O", "CaSO4·2H2O", bd("172.164"),
            listOf(Nutrient("Ca", bd("0.23279")), Nutrient("S", bd("0.18622"))), Category.GH_KH),
        Compound("CaCl2", "CaCl2", bd("110.978"),
            listOf(Nutrient("Ca", bd("0.36114"))), Category.REEF),
        Compound("NaHCO3", "NaHCO3", bd("84.006"),
            listOf(Nutrient("HCO3", bd("0.72633"))), Category.REEF),
        Compound("MgCl2_6H2O", "MgCl2·6H2O", bd("203.295"),
            listOf(Nutrient("Mg", bd("0.11956"))), Category.REEF),
    ).associateBy { it.id }

    /** ppm increase per nutrient for [doseGrams] of [compoundId] dosed into [volumeLitres]. */
    fun ppmIncrease(compoundId: String, doseGrams: BigDecimal, volumeLitres: BigDecimal): CalcResult<List<PpmResult>> {
        val compound = CATALOG[compoundId]
            ?: return CalcResult.Unsupported("Unknown compound: $compoundId", evidenceGap = "not in verified WS3 catalog")
        ValidationEngine.requireVolumeLitres(volumeLitres).failureOrNull()?.let { return it }
        if (doseGrams.signum() < 0) return CalcResult.CalculationError("NEGATIVE_DOSE", "dose < 0")
        val results = compound.nutrients.map { n ->
            // ppm = dose * fraction * 1000 / volume
            val ppm = doseGrams.multiply(n.massFraction, MC).multiply(THOUSAND, MC).divide(volumeLitres, MC)
            PpmResult(n.symbol, ppm)
        }
        return CalcResult.Success(results)
    }

    /** Grams of [compoundId] needed to raise [nutrientSymbol] by [targetPpm] in [volumeLitres]. */
    fun gramsForTargetPpm(
        compoundId: String,
        nutrientSymbol: String,
        targetPpm: BigDecimal,
        volumeLitres: BigDecimal,
    ): CalcResult<BigDecimal> {
        val compound = CATALOG[compoundId]
            ?: return CalcResult.Unsupported("Unknown compound: $compoundId")
        val nutrient = compound.nutrients.firstOrNull { it.symbol == nutrientSymbol }
            ?: return CalcResult.Unsupported("$compoundId does not supply $nutrientSymbol")
        ValidationEngine.requireVolumeLitres(volumeLitres).failureOrNull()?.let { return it }
        if (targetPpm.signum() < 0) return CalcResult.CalculationError("NEGATIVE_TARGET", "targetPpm < 0")
        // grams = targetPpm * volume / (fraction * 1000)
        val grams = targetPpm.multiply(volumeLitres, MC)
            .divide(nutrient.massFraction.multiply(THOUSAND, MC), MC)
        return CalcResult.Success(grams)
    }
}
