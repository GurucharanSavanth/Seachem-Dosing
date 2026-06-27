package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.domain.model.DosingResult
import com.example.seachem_dosing.logic.SeachemCalculations
import java.math.BigDecimal

/**
 * Façade over the dual calculation engines
 * ([com.example.seachem_dosing.logic.Calculations] +
 *  [com.example.seachem_dosing.logic.SeachemCalculations]).
 *
 * UseCases / ViewModels depend on this contract; concrete impl wraps the
 * static `object` singletons. Lets tests substitute a mock engine.
 *
 * Calculation sync invariant (CLAUDE.md): formulas live in BOTH
 * `Calculations.kt` and `Base_Template/js/dosingCalculations.js`. This
 * Repository façade does not duplicate them — it delegates.
 */
interface CalculationsRepository {

    /** Reef / Flourish / buffer products via [SeachemCalculations]. */
    fun calculateForProduct(
        product: SeachemCalculations.Product,
        currentValue: BigDecimal,
        targetValue: BigDecimal,
        volumeLitres: BigDecimal,
        scale: SeachemCalculations.UnitScale
    ): DosingResult

    /** Emergency / utility doses via the simpler `Calculations` object. */
    fun calculatePrimeDose(volumeLitres: Double): Double
    fun calculateStabilityDose(volumeLitres: Double): Double
    fun calculateSafe(volumeLitres: Double): Double
    fun calculateAptComplete(volumeLitres: Double, currentNitrate: Double): AptResult

    data class AptResult(
        val ml: Double,
        val estimatedNitrateIncrease: Double,
        val estimatedFinalNitrate: Double
    )
}
