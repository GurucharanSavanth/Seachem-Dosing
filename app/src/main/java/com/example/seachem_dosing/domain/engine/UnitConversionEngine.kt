package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Unit-safe volume + hardness conversions in BigDecimal (SPEC §V2, §V9).
 *
 * Coefficients mirror [com.example.seachem_dosing.logic.Calculations] and
 * `Base_Template/js/utils.js` to preserve the cross-platform parity invariant
 * (§V10). Division uses a [MathContext] so non-terminating quotients (e.g.
 * /3.78541) round instead of throwing ArithmeticException.
 */
object UnitConversionEngine {

    val US_GAL_TO_L: BigDecimal = BigDecimal("3.78541")   // parity: Calculations.US_GAL_TO_L
    val UK_GAL_TO_L: BigDecimal = BigDecimal("4.54609")   // parity: Calculations.UK_GAL_TO_L
    val PPM_TO_DH: BigDecimal = BigDecimal("17.86")       // parity: Calculations.PPM_TO_DH

    // ponytail: 12 significant digits is well past display precision; HALF_UP matches Calculations rounding.
    private val MC = MathContext(12, RoundingMode.HALF_UP)

    enum class VolumeUnit { L, US_GAL, UK_GAL }

    fun toLitres(volume: BigDecimal, unit: VolumeUnit): CalcResult<BigDecimal> {
        if (volume.signum() < 0) return CalcResult.CalculationError("NEGATIVE_VOLUME", "volume < 0")
        val litres = when (unit) {
            VolumeUnit.L -> volume
            VolumeUnit.US_GAL -> volume.multiply(US_GAL_TO_L, MC)
            VolumeUnit.UK_GAL -> volume.multiply(UK_GAL_TO_L, MC)
        }
        return CalcResult.Success(litres)
    }

    fun fromLitres(litres: BigDecimal, unit: VolumeUnit): CalcResult<BigDecimal> {
        if (litres.signum() < 0) return CalcResult.CalculationError("NEGATIVE_VOLUME", "litres < 0")
        val value = when (unit) {
            VolumeUnit.L -> litres
            VolumeUnit.US_GAL -> litres.divide(US_GAL_TO_L, MC)
            VolumeUnit.UK_GAL -> litres.divide(UK_GAL_TO_L, MC)
        }
        return CalcResult.Success(value)
    }

    fun ppmToDh(ppm: BigDecimal): CalcResult<BigDecimal> {
        if (ppm.signum() < 0) return CalcResult.CalculationError("NEGATIVE_VALUE", "ppm < 0")
        return CalcResult.Success(ppm.divide(PPM_TO_DH, MC))
    }

    fun dhToPpm(dh: BigDecimal): CalcResult<BigDecimal> {
        if (dh.signum() < 0) return CalcResult.CalculationError("NEGATIVE_VALUE", "dH < 0")
        return CalcResult.Success(dh.multiply(PPM_TO_DH, MC))
    }
}
