package com.example.seachem_dosing.core.result

/**
 * Outcome of any user-facing calculation (dosing, fertilizer, medication).
 *
 * Enforces SPEC §V1 + §V3: engines never throw to the UI and never return a
 * bare number — every result is exactly one of these typed cases. Callers use
 * exhaustive `when` expressions, so a new failure mode cannot be silently ignored.
 */
sealed interface CalcResult<out T> {

    /** Computation succeeded. [value] is the domain result; [warnings] are non-fatal advisories. */
    data class Success<T>(val value: T, val warnings: List<String> = emptyList()) : CalcResult<T>

    /** Cannot compute yet — caller must supply [required] inputs first (§V3/§V5). */
    data class NeedsMoreInput(val required: List<String>, val reason: String) : CalcResult<Nothing>

    /** Refused on safety grounds — e.g. FW med in SW, copper with invertebrates (§V4/§V5). */
    data class UnsafeBlocked(
        val reason: String,
        val evidence: String? = null,
        val escalation: String? = null,
    ) : CalcResult<Nothing>

    /** Out of supported scope — unknown product, or unverified compatibility (§V4). */
    data class Unsupported(val reason: String, val evidenceGap: String? = null) : CalcResult<Nothing>

    /** Numeric/domain error (overflow, NaN, negative, bad unit). [debugMessage] is log-safe (§V3). */
    data class CalculationError(val errorType: String, val debugMessage: String) : CalcResult<Nothing>
}

/**
 * The non-success cases re-typed to [Nothing] (value absent), or null if Success.
 * Lets engines short-circuit input guards: `validate(x).failureOrNull()?.let { return it }`.
 */
fun CalcResult<*>.failureOrNull(): CalcResult<Nothing>? = when (this) {
    is CalcResult.Success -> null
    is CalcResult.NeedsMoreInput -> this
    is CalcResult.UnsafeBlocked -> this
    is CalcResult.Unsupported -> this
    is CalcResult.CalculationError -> this
}
