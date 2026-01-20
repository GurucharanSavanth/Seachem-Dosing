package com.example.seachem_dosing.logic

import kotlin.math.max
import kotlin.math.min

object Calculations {

    // --- CONSTANTS ---
    const val US_GAL_TO_L = 3.78541
    const val UK_GAL_TO_L = 4.54609

    // Dimension to volume conversions
    const val CM3_TO_L = 0.001        // 1 cm³ = 0.001 L
    const val IN3_TO_L = 0.0163871    // 1 in³ = 0.0163871 L
    const val FT3_TO_L = 28.3168      // 1 ft³ = 28.3168 L

    const val PPM_TO_DH = 17.86

    // Coefficients
    const val COEFF_KHCO3_STOICH = 0.0357
    const val COEFF_EQUILIBRIUM = 16.0 / (80.0 * 3.0) // ~0.0667
    const val GPL_MIN_NR = 0.0625
    const val GPL_MAX_NR = 0.125
    const val COEFF_ACID = 1.5 / (40.0 * 2.8) // ~0.01339
    const val COEFF_GOLD_FULL = 6.0 / 40.0 // 0.15
    const val COEFF_SAFE = 1.0 / 200.0 // 0.005
    const val COEFF_APT_80PCT = 0.8 * (3.0 / 100.0) // 0.024
    const val APT_NITRATE_EST_PER_ML = 1.5

    // Emergency Dosing
    const val PRIME_ML_PER_L = 5.0 / 200.0
    const val STABILITY_ML_PER_L = 5.0 / 40.0

    // Minimum purity threshold to prevent division issues
    private const val MIN_PURITY = 0.01

    // --- CONVERSIONS ---

    fun toLitres(volume: Double, unit: String): Double {
        return when (unit) {
            "US" -> volume * US_GAL_TO_L
            "UK" -> volume * UK_GAL_TO_L
            else -> volume // "L"
        }
    }

    fun fromLitres(litres: Double, unit: String): Double {
        return when (unit) {
            "US" -> litres / US_GAL_TO_L
            "UK" -> litres / UK_GAL_TO_L
            else -> litres // "L"
        }
    }

    fun dimensionsToLitres(length: Double, breadth: Double, height: Double, unit: String): Double {
        val volume = length * breadth * height
        if (volume <= 0) return 0.0
        return when (unit) {
            "in" -> volume * IN3_TO_L
            "ft" -> volume * FT3_TO_L
            else -> volume * CM3_TO_L // "cm"
        }
    }

    fun ppmToDh(ppm: Double): Double = if (ppm <= 0) 0.0 else ppm / PPM_TO_DH
    fun dhToPpm(dh: Double): Double = if (dh <= 0) 0.0 else dh * PPM_TO_DH

    // --- CALCULATORS ---

    fun calculateKhco3Grams(currentKh: Double, targetKh: Double, litres: Double, purity: Double): Double {
        if (litres <= 0) return 0.0
        // Use minimum purity to prevent division by very small numbers
        val safePurity = max(purity, MIN_PURITY)
        val grams = (targetKh - currentKh) * COEFF_KHCO3_STOICH * litres / safePurity
        return max(0.0, grams)
    }

    fun calculateEquilibriumGrams(deltaGh: Double, litres: Double): Double {
        if (litres <= 0 || deltaGh <= 0) return 0.0
        val grams = deltaGh * COEFF_EQUILIBRIUM * litres
        return max(0.0, grams)
    }

    fun calculateNeutralRegulatorGrams(litres: Double, currentPh: Double, targetPh: Double, currentKh: Double): Double {
        if (litres <= 0) return 0.0
        if (targetPh >= currentPh) return 0.0
        val safeKh = max(0.0, currentKh)
        val khEffectFactor = min(safeKh, 4.0) / 4.0
        val baseGramsPerLitre = GPL_MIN_NR + (GPL_MAX_NR - GPL_MIN_NR) * khEffectFactor
        val phSteps = (currentPh - targetPh) / 0.5
        if (phSteps <= 0) return 0.0
        var grams = baseGramsPerLitre * litres * phSteps
        grams = min(grams, GPL_MAX_NR * litres * 2.0)
        return max(0.0, grams)
    }

    fun calculateAcidBufferGrams(litres: Double, currentKh: Double, targetKh: Double): Double {
        if (litres <= 0) return 0.0
        val deltaKh = currentKh - targetKh
        if (deltaKh <= 0) return 0.0
        val grams = deltaKh * COEFF_ACID * litres
        return max(0.0, grams)
    }

    data class GoldBufferResult(val grams: Double, val fullDose: Boolean)

    fun calculateGoldBufferGrams(litres: Double, currentPh: Double, targetPh: Double): GoldBufferResult {
        if (litres <= 0) return GoldBufferResult(0.0, false)
        val deltaPh = targetPh - currentPh
        if (deltaPh <= 0) return GoldBufferResult(0.0, false)
        val fullDoseRecommended = deltaPh >= 0.3
        val doseMultiplier = if (fullDoseRecommended) 1.0 else 0.5
        val grams = COEFF_GOLD_FULL * doseMultiplier * litres
        return GoldBufferResult(max(0.0, grams), fullDoseRecommended)
    }

    fun calculateSafeGrams(litres: Double): Double {
        if (litres <= 0) return 0.0
        val grams = litres * COEFF_SAFE
        return max(0.0, grams)
    }

    fun calculatePrimeDose(litres: Double): Double = max(0.0, litres * PRIME_ML_PER_L)
    fun calculateStabilityDose(litres: Double): Double = max(0.0, litres * STABILITY_ML_PER_L)

    data class AptResult(val ml: Double, val estimatedNitrateIncrease: Double, val estimatedFinalNitrate: Double)

    fun calculateAptCompleteDose(litres: Double, currentNitrate: Double = 0.0): AptResult {
        if (litres <= 0) return AptResult(0.0, 0.0, max(0.0, currentNitrate))
        val ml = litres * COEFF_APT_80PCT
        // Nitrate contribution: approximately 0.015 ppm per ml dosed
        val nitrateContribution = ml * 0.015
        val safeCurrentNitrate = max(0.0, currentNitrate)
        val estimatedFinalNitrate = safeCurrentNitrate + nitrateContribution
        return AptResult(
            max(0.0, ml),
            max(0.0, nitrateContribution),
            max(0.0, estimatedFinalNitrate)
        )
    }
}
