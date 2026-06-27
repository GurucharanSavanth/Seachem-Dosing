package com.example.seachem_dosing.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SPEC §V1: the five-state result is exhaustive and its combinators are correct. */
class CalcResultTest {

    @Test
    fun map_transforms_success_value() {
        val r: CalcResult<Int> = CalcResult.Success(2, warnings = listOf("w"))
        val m = r.map { it * 3 }
        assertTrue(m is CalcResult.Success)
        assertEquals(6, (m as CalcResult.Success).value)
        assertEquals(listOf("w"), m.warnings)   // warnings preserved
    }

    @Test
    fun map_passes_through_non_success() {
        val r: CalcResult<Int> = CalcResult.NeedsMoreInput(listOf("volume"), "need volume")
        assertTrue(r.map { it * 3 } is CalcResult.NeedsMoreInput)
    }

    @Test
    fun fold_hits_unsafe_blocked_branch() {
        val r: CalcResult<Int> = CalcResult.UnsafeBlocked("copper + inverts")
        val s = r.fold(
            onSuccess = { _, _ -> "ok" },
            onNeedsMoreInput = { _, _ -> "need" },
            onUnsafeBlocked = { reason, _, _ -> "blocked:$reason" },
            onUnsupported = { _, _ -> "unsupported" },
            onError = { _, _ -> "error" },
        )
        assertEquals("blocked:copper + inverts", s)
    }

    @Test
    fun isSuccess_reflects_case() {
        assertTrue(CalcResult.Success(1).isSuccess)
        assertTrue(!CalcResult.CalculationError("X", "y").isSuccess)
    }
}
