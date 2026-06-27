package com.example.seachem_dosing.domain.engine

import com.example.seachem_dosing.core.result.CalcResult
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/** SPEC §V3: bad inputs become typed results, never exceptions or silent zero. */
class ValidationEngineTest {

    @Test fun null_volume_needs_more_input() {
        assertTrue(ValidationEngine.requireVolumeLitres(null) is CalcResult.NeedsMoreInput)
    }

    @Test fun zero_volume_needs_more_input() {
        assertTrue(ValidationEngine.requireVolumeLitres(BigDecimal.ZERO) is CalcResult.NeedsMoreInput)
    }

    @Test fun negative_volume_needs_more_input() {
        assertTrue(ValidationEngine.requireVolumeLitres(BigDecimal("-5")) is CalcResult.NeedsMoreInput)
    }

    @Test fun absurdly_large_volume_is_error() {
        assertTrue(ValidationEngine.requireVolumeLitres(BigDecimal("9999999999")) is CalcResult.CalculationError)
    }

    @Test fun valid_volume_succeeds() {
        assertTrue(ValidationEngine.requireVolumeLitres(BigDecimal("200")) is CalcResult.Success)
    }

    @Test fun purity_above_one_is_error() {
        assertTrue(ValidationEngine.requirePurityFraction(BigDecimal("1.5")) is CalcResult.CalculationError)
    }

    @Test fun zero_purity_needs_more_input() {
        assertTrue(ValidationEngine.requirePurityFraction(BigDecimal.ZERO) is CalcResult.NeedsMoreInput)
    }
}
