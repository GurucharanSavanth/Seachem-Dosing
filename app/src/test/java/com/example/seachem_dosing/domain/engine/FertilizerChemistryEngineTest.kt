package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** SPEC §V2/§V3/§V7 — chemistry numbers match DEEP_RESEARCH_REPORT WS3 worked examples. */
class FertilizerChemistryEngineTest {

    private fun ppmOf(r: CalcResult<List<FertilizerChemistryEngine.PpmResult>>, symbol: String): Double =
        (r as CalcResult.Success).value.first { it.nutrient == symbol }.ppm.toDouble()

    @Test fun kno3_one_gram_in_100L_matches_research() {
        // WS3: 1 g KNO3 in 100 L → N 1.385 ppm, K 3.867 ppm
        val r = FertilizerChemistryEngine.ppmIncrease("KNO3", BigDecimal("1"), BigDecimal("100"))
        assertEquals(1.3853, ppmOf(r, "NO3-N"), 1e-3)
        assertEquals(3.8672, ppmOf(r, "K"), 1e-3)
    }

    @Test fun grams_for_target_round_trips() {
        // Raise N by 1.3853 ppm in 100 L of KNO3 → ~1.0 g
        val r = FertilizerChemistryEngine.gramsForTargetPpm("KNO3", "NO3-N", BigDecimal("1.3853"), BigDecimal("100"))
        assertEquals(1.0, (r as CalcResult.Success).value.toDouble(), 1e-4)
    }

    @Test fun unknown_compound_is_unsupported() {   // §V (no invented chemistry)
        assertTrue(
            FertilizerChemistryEngine.ppmIncrease("UNOBTAINIUM", BigDecimal("1"), BigDecimal("100"))
                is CalcResult.Unsupported
        )
    }

    @Test fun zero_volume_needs_more_input() {   // §V3 via ValidationEngine
        assertTrue(
            FertilizerChemistryEngine.ppmIncrease("KNO3", BigDecimal("1"), BigDecimal.ZERO)
                is CalcResult.NeedsMoreInput
        )
    }

    @Test fun negative_dose_is_calculation_error() {   // §V3
        assertTrue(
            FertilizerChemistryEngine.ppmIncrease("KNO3", BigDecimal("-1"), BigDecimal("100"))
                is CalcResult.CalculationError
        )
    }

    @Test fun nutrient_not_supplied_is_unsupported() {
        assertTrue(
            FertilizerChemistryEngine.gramsForTargetPpm("KNO3", "Fe", BigDecimal("1"), BigDecimal("100"))
                is CalcResult.Unsupported
        )
    }
}
