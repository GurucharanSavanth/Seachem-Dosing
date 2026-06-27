package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import java.math.BigDecimal

/**
 * Input validation shared by all calculation engines (SPEC §V3, §V5).
 * Returns typed [CalcResult] (NeedsMoreInput / CalculationError) — never throws.
 */
object ValidationEngine {

    // Sanity bounds: nano shrimp tank → large public system. Upper bound also guards typos/overflow.
    val MIN_VOLUME_L: BigDecimal = BigDecimal("0.1")
    val MAX_VOLUME_L: BigDecimal = BigDecimal("1000000")

    /** Volume must be present, > 0, and within sane bounds (§V3). */
    fun requireVolumeLitres(volume: BigDecimal?): CalcResult<BigDecimal> = when {
        volume == null -> CalcResult.NeedsMoreInput(listOf("volume"), "Tank volume is required.")
        volume.signum() <= 0 -> CalcResult.NeedsMoreInput(listOf("volume"), "Tank volume must be greater than zero.")
        volume < MIN_VOLUME_L -> CalcResult.CalculationError("VOLUME_TOO_SMALL", "volume < $MIN_VOLUME_L L")
        volume > MAX_VOLUME_L -> CalcResult.CalculationError("VOLUME_TOO_LARGE", "volume > $MAX_VOLUME_L L")
        else -> CalcResult.Success(volume)
    }

    /** A measured/target parameter must be present and non-negative (§V3). */
    fun requireNonNegative(value: BigDecimal?, field: String): CalcResult<BigDecimal> = when {
        value == null -> CalcResult.NeedsMoreInput(listOf(field), "$field is required.")
        value.signum() < 0 -> CalcResult.CalculationError("NEGATIVE_VALUE", "$field < 0")
        else -> CalcResult.Success(value)
    }

    /** Concentration/purity must be a fraction in (0, 1] before any dose math (§V3). */
    fun requirePurityFraction(purity: BigDecimal?): CalcResult<BigDecimal> = when {
        purity == null -> CalcResult.NeedsMoreInput(listOf("purity"), "Product strength/purity is required.")
        purity.signum() <= 0 -> CalcResult.NeedsMoreInput(listOf("purity"), "Purity must be greater than zero.")
        purity > BigDecimal.ONE -> CalcResult.CalculationError("PURITY_OUT_OF_RANGE", "purity > 1.0")
        else -> CalcResult.Success(purity)
    }
}
