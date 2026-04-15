package com.example.seachem_dosing.logic

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeachemCalculationsTest {

    @Test
    fun alkalineBuffer_dkhInputMatchesEquivalentMeqInput() {
        val dkhResult = SeachemCalculations.calculateAlkalineBuffer(
            current = BigDecimal("6.0"),
            desired = BigDecimal("8.0"),
            volume = BigDecimal("100.0"),
            volumeUnit = "L",
            inputScale = SeachemCalculations.UnitScale.DKH
        )
        val meqResult = SeachemCalculations.calculateAlkalineBuffer(
            current = BigDecimal("2.142857"),
            desired = BigDecimal("2.857143"),
            volume = BigDecimal("100.0"),
            volumeUnit = "L",
            inputScale = SeachemCalculations.UnitScale.MEQ_L
        )

        assertEquals(meqResult.primaryValue.toDouble(), dkhResult.primaryValue.toDouble(), 0.001)
        assertTrue(dkhResult.primaryValue.toDouble() > 0.0)
    }

    @Test
    fun saltMix_rejectsNonIncreasingTargetSalinity() {
        val result = SaltMixCalculations.calculateSaltMix(
            productName = "Red Sea Coral Pro",
            volumeGallons = 5.0,
            currentPpt = 35.0,
            desiredPpt = 34.0
        )

        assertEquals(null, result)
    }
}
