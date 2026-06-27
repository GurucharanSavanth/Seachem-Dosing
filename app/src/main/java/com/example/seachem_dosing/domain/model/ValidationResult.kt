package com.example.seachem_dosing.domain.model

/**
 * Outcome of an input validation check.
 *
 * - [Valid] — input passes; no further action needed.
 * - [Invalid] — input fails; surfaces field id + human-readable reason so
 *   the UI can highlight the specific field and show inline error text.
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val field: String, val reason: String) : ValidationResult()
}
