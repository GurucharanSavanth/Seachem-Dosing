package com.example.seachem_dosing.logic

import java.math.BigDecimal
import java.math.RoundingMode

object SaltMixCalculations {

    data class SaltMixResult(
        val productName: String,
        val factor: Double,
        val grams: Double,
        val kilograms: Double,
        val pounds: Double
    )

    // Factor unit: grams needed per (gallon * 1 PPT)
    val SALT_MIX_PRODUCTS = mapOf(
        "Aquaforest Hybrid Pro Salt Mix" to 4.3769,
        "Aquaforest Reef Salt Mix" to 4.3769,
        "Aquaforest Reef Salt Plus Mix" to 4.3769,
        "Aquaforest Sea Salt Mix" to 4.3769,
        "Brightwell NeoMarine" to 3.971428571,
        "HW-Marinemix Professional" to 3.857142857,
        "HW-Marinemix Reefer" to 3.857142857,
        "Instant Ocean Sea Salt Mix" to 4.285714286,
        "Instant Ocean Reef Crystals" to 4.285714286,
        "Nyos Pure Salt Mix" to 4.339,
        "Red Sea Coral Pro" to 4.114285714,
        "Red Sea Blue Bucket" to 4.114285714,
        "Reef Crystals" to 4.285714286,
        "Tropic Marin Bio-Actif" to 4.2,
        "Tropic Marin Classic" to 4.2,
        "Tropic Marin Pro Reef" to 4.2,
        "Tropic Marin SynBiotic" to 4.2
    )

    private const val GRAMS_TO_POUNDS = 0.00220462

    /**
     * Calculates the required salt mix mass.
     *
     * @param productName Name of the product (must exist in SALT_MIX_PRODUCTS).
     * @param volumeGallons Volume in US Gallons.
     * @param currentPpt Current salinity in PPT.
     * @param desiredPpt Desired salinity in PPT.
     * @return Result object with calculated masses, or null if inputs invalid.
     */
    fun calculateSaltMix(
        productName: String,
        volumeGallons: Double,
        currentPpt: Double,
        desiredPpt: Double
    ): SaltMixResult? {
        if (volumeGallons <= 0 || currentPpt < 0 || desiredPpt < 0 || desiredPpt <= currentPpt) {
            return null
        }

        val factor = SALT_MIX_PRODUCTS[productName] ?: return null

        val deltaPpt = desiredPpt - currentPpt
        val gramsRaw = deltaPpt * volumeGallons * factor
        val kilogramsRaw = gramsRaw / 1000.0
        val poundsRaw = gramsRaw * GRAMS_TO_POUNDS

        return SaltMixResult(
            productName = productName,
            factor = factor,
            grams = BigDecimal(gramsRaw).setScale(1, RoundingMode.HALF_UP).toDouble(),
            kilograms = BigDecimal(kilogramsRaw).setScale(3, RoundingMode.HALF_UP).toDouble(),
            pounds = BigDecimal(poundsRaw).setScale(3, RoundingMode.HALF_UP).toDouble()
        )
    }
}
