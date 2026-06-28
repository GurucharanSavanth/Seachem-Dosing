package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.R
import com.example.seachem_dosing.domain.engine.RecommendationEngine.FreshwaterInput
import com.example.seachem_dosing.domain.engine.RecommendationEngine.Line
import com.example.seachem_dosing.domain.engine.RecommendationEngine.SaltwaterInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parity lock for dashboard advice thresholds (SPEC — logic must not silently drift). */
class RecommendationEngineTest {

    private fun fwParam(r: RecommendationEngine.Report, labelRes: Int) =
        r.details.filterIsInstance<Line.Param>().first { it.labelRes == labelRes }

    @Test
    fun freshwater_clean_water_has_no_danger_actions() {
        val r = RecommendationEngine.freshwater(
            FreshwaterInput(litres = 100.0, ammonia = 0.0, nitrite = 0.0, nitrate = 15.0,
                ghDegrees = 6.0, khDegrees = 5.0, ph = 7.2, temp = 26.0, potassium = 20.0, iron = 0.2)
        )
        assertTrue(r.actions.none { it.res == R.string.reco_action_volume_needed })
        assertTrue(r.actions.none { it.res == R.string.reco_action_ammonia })
        assertEquals(R.string.reco_msg_ammonia_ok, fwParam(r, R.string.param_ammonia).msgRes)
    }

    @Test
    fun freshwater_ammonia_present_with_volume_gives_dosed_action() {
        val r = RecommendationEngine.freshwater(
            FreshwaterInput(100.0, 0.5, 0.0, 15.0, 6.0, 5.0, 7.2, 26.0, 20.0, 0.2)
        )
        val a = r.actions.first { it.res == R.string.reco_action_ammonia }
        assertNotNull(a.arg) // Prime dose computed + formatted
    }

    @Test
    fun freshwater_zero_volume_uses_generic_actions() {
        val r = RecommendationEngine.freshwater(
            FreshwaterInput(0.0, 0.5, 0.0, 15.0, 6.0, 5.0, 7.2, 26.0, 20.0, 0.2)
        )
        assertTrue(r.actions.any { it.res == R.string.reco_action_volume_needed })
        assertTrue(r.actions.any { it.res == R.string.reco_action_ammonia_generic })
    }

    @Test
    fun freshwater_high_nitrate_flags_action_and_detail() {
        val r = RecommendationEngine.freshwater(
            FreshwaterInput(100.0, 0.0, 0.0, 60.0, 6.0, 5.0, 7.2, 26.0, 20.0, 0.2)
        )
        assertTrue(r.actions.any { it.res == R.string.reco_action_nitrate_high })
        assertEquals(R.string.reco_msg_nitrate_high, fwParam(r, R.string.param_nitrate).msgRes)
    }

    @Test
    fun saltwater_high_nitrate_flags_reef_action() {
        val r = RecommendationEngine.saltwater(
            SaltwaterInput(100.0, 35.0, 8.0, 420.0, 1300.0, 25.0, 0.05, 8.2, 26.0, 8.0, 0.06)
        )
        assertTrue(r.actions.any { it.res == R.string.reco_action_nitrate_reef_high })
    }

    @Test
    fun pond_zero_volume_flags_volume_and_has_plain_detail() {
        val r = RecommendationEngine.pond(0.0)
        assertTrue(r.actions.any { it.res == R.string.reco_action_volume_needed })
        assertTrue(r.details.any { it is Line.Plain })
    }
}
