package com.example.seachem_dosing.domain.model

import java.math.BigDecimal

/**
 * Outcome of a dosing calculation.
 *
 * - [Success] holds primary measurement (e.g. "12.345 g") plus alternate-unit
 *   secondary readout (e.g. "2.469 tsp") and any safety warnings.
 * - [Error] carries a code + message + recoverable flag for UI error handling.
 *
 * Replaces [com.example.seachem_dosing.logic.SeachemCalculations.CalculationResult]
 * (a plain data class with no error path) at the Repository / UseCase boundary.
 */
sealed class DosingResult {

    data class Success(
        val primaryValue: BigDecimal,
        val primaryUnit: String,
        val secondaryValue: BigDecimal,
        val secondaryUnit: String,
        val warnings: List<String> = emptyList()
    ) : DosingResult()

    data class Error(
        val code: String,
        val message: String,
        val recoverable: Boolean = true
    ) : DosingResult()
}
