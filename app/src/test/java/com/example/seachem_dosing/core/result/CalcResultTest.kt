package com.example.seachem_dosing.core.result

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** SPEC §V1: the five-state result can short-circuit non-success cases. */
class CalcResultTest {

    @Test
    fun failureOrNull_returns_null_for_success() {
        assertNull(CalcResult.Success(1).failureOrNull())
    }

    @Test
    fun failureOrNull_returns_same_non_success_case() {
        val cases = listOf(
            CalcResult.NeedsMoreInput(listOf("volume"), "need volume"),
            CalcResult.UnsafeBlocked("copper + inverts"),
            CalcResult.Unsupported("unknown product"),
            CalcResult.CalculationError("X", "debug"),
        )

        cases.forEach { assertSame(it, it.failureOrNull()) }
    }
}
