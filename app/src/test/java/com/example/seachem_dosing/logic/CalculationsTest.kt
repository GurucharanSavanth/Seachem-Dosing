package com.example.seachem_dosing.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculationsTest {

    @Test
    fun toLitres_convertsSupportedVolumeUnits() {
        assertEquals(37.8541, Calculations.toLitres(10.0, "US"), 0.0001)
        assertEquals(45.4609, Calculations.toLitres(10.0, "UK"), 0.0001)
        assertEquals(10.0, Calculations.toLitres(10.0, "L"), 0.0001)
    }

    @Test
    fun dimensionsToLitres_handlesCmInAndFt() {
        assertEquals(72.0, Calculations.dimensionsToLitres(60.0, 30.0, 40.0, "cm"), 0.0001)
        assertEquals(16.3871, Calculations.dimensionsToLitres(10.0, 10.0, 10.0, "in"), 0.0001)
        assertEquals(28.3168, Calculations.dimensionsToLitres(1.0, 1.0, 1.0, "ft"), 0.0001)
    }

    @Test
    fun hardnessConversion_roundTripsDegreesAndPpm() {
        val ppm = Calculations.dhToPpm(6.0)
        assertEquals(107.16, ppm, 0.0001)
        assertEquals(6.0, Calculations.ppmToDh(ppm), 0.0001)
    }
}
