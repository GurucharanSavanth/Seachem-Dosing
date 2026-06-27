package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import com.example.seachem_dosing.domain.engine.UnitConversionEngine.VolumeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** SPEC §V2 (BigDecimal), §V3 (typed errors), §V9 (round-trip tolerance). */
class UnitConversionEngineTest {

    private fun value(r: CalcResult<BigDecimal>): BigDecimal =
        (r as CalcResult.Success).value

    @Test
    fun us_gallons_to_litres_matches_coefficient() {
        val l = value(UnitConversionEngine.toLitres(BigDecimal("10"), VolumeUnit.US_GAL))
        assertEquals(37.8541, l.toDouble(), 1e-6)
    }

    @Test
    fun uk_gallons_to_litres_matches_coefficient() {
        val l = value(UnitConversionEngine.toLitres(BigDecimal("10"), VolumeUnit.UK_GAL))
        assertEquals(45.4609, l.toDouble(), 1e-6)
    }

    @Test
    fun litres_round_trip_us_within_tolerance() {   // §V9
        val v = BigDecimal("57")
        val l = value(UnitConversionEngine.toLitres(v, VolumeUnit.US_GAL))
        val back = value(UnitConversionEngine.fromLitres(l, VolumeUnit.US_GAL))
        assertEquals(v.toDouble(), back.toDouble(), 1e-6)
    }

    @Test
    fun ppm_dh_round_trip() {   // §V9
        val ppm = BigDecimal("100")
        val dh = value(UnitConversionEngine.ppmToDh(ppm))
        assertEquals(100.0, value(UnitConversionEngine.dhToPpm(dh)).toDouble(), 1e-6)
    }

    @Test
    fun negative_volume_is_calculation_error() {   // §V3
        assertTrue(
            UnitConversionEngine.toLitres(BigDecimal("-1"), VolumeUnit.L) is CalcResult.CalculationError
        )
    }

    @Test
    fun negative_ppm_is_calculation_error() {   // §V3
        assertTrue(UnitConversionEngine.ppmToDh(BigDecimal("-5")) is CalcResult.CalculationError)
    }
}
