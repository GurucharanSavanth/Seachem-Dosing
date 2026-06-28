package com.example.seachem_dosing.domain.medication

import com.example.seachem_dosing.core.result.CalcResult
import com.example.seachem_dosing.domain.medication.MedicationSafetyEngine.MedAdvice
import com.example.seachem_dosing.domain.medication.MedicationSafetyEngine.TankContext
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** Medication safety gates (SPEC §V4–V6) — refuse, don't guess. */
class MedicationSafetyEngineTest {

    private fun full(wt: WaterType, inverts: Boolean = false, prior: List<String> = emptyList()) =
        TankContext(
            waterType = wt, volumeLitres = BigDecimal("100"), hasInvertsOrCorals = inverts,
            filtrationAcknowledged = true, speciesConfirmed = true, priorActives = prior,
        )

    private fun med(id: String) = MedicationCatalog.byId(id)!!

    @Test
    fun missing_context_for_high_risk_needs_more_input() {  // §V5
        val r = MedicationSafetyEngine.assess(med("seachem_kanaplex"), TankContext())
        assertTrue(r is CalcResult.NeedsMoreInput)
    }

    @Test
    fun freshwater_only_med_in_saltwater_is_blocked() {  // §V4
        val r = MedicationSafetyEngine.assess(med("api_em_erythromycin"), full(WaterType.SALTWATER))
        assertTrue(r is CalcResult.UnsafeBlocked)
    }

    @Test
    fun copper_with_invertebrates_is_blocked() {  // §V5
        val r = MedicationSafetyEngine.assess(med("seachem_cupramine"), full(WaterType.SALTWATER, inverts = true))
        assertTrue(r is CalcResult.UnsafeBlocked)
    }

    @Test
    fun duplicate_active_ingredient_is_blocked() {
        val r = MedicationSafetyEngine.assess(med("seachem_metroplex"), full(WaterType.FRESHWATER, prior = listOf("metronidazole")))
        assertTrue(r is CalcResult.UnsafeBlocked)
    }

    @Test
    fun valid_context_succeeds_and_surfaces_carbon_removal() {  // §V6
        val r = MedicationSafetyEngine.assess(med("seachem_kanaplex"), full(WaterType.FRESHWATER))
        assertTrue(r is CalcResult.Success)
        val warnings = (r as CalcResult.Success<MedAdvice>).value.warnings
        assertTrue(warnings.any { it.contains("carbon", ignoreCase = true) })
    }

    @Test
    fun unverified_dose_is_flagged_in_warnings() {  // Kordon aquarium dose pending (#14)
        val r = MedicationSafetyEngine.assess(med("kordon_rid_ich_plus"), full(WaterType.FRESHWATER))
        assertTrue(r is CalcResult.Success)
        val warnings = (r as CalcResult.Success<MedAdvice>).value.warnings
        assertTrue(warnings.any { it.contains("verify", ignoreCase = true) || it.contains("confirm", ignoreCase = true) })
    }
}
