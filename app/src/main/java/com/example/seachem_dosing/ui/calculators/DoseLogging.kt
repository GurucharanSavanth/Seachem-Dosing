package com.example.seachem_dosing.ui.calculators

import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.logic.SeachemCalculations.CalculationResult

/** A calculator dose ready to log as administered: the physical (g/mL) amount + tank volume. */
data class DoseLogRequest(
    val productId: String,
    val amount: StoredDecimal,
    val unit: UnitCode,
    val volumeLitres: Double,
)

/**
 * Builds a [DoseLogRequest] from a calculator [result], using the **physical** g/mL amount — the
 * engine always yields one as primary or secondary alongside the display spoon, and spoons
 * (`tsp`/`caps`/`tbsp`) are product-specific measures with no exact unit (ADR-011 §11). Returns null
 * when no positive g/mL amount exists, so the "Log as administered" action is hidden.
 */
internal fun doseLogRequestFrom(
    productId: String,
    result: CalculationResult,
    volumeLitres: Double,
): DoseLogRequest? {
    fun unitOf(code: String): UnitCode? = when (code) {
        "g" -> UnitCode.GRAM
        "mL" -> UnitCode.MILLILITER
        else -> null
    }
    val primary = unitOf(result.primaryUnit)
    val secondary = unitOf(result.secondaryUnit)
    val (value, unit) = when {
        primary != null && result.primaryValue.signum() > 0 -> result.primaryValue to primary
        secondary != null && result.secondaryValue.signum() > 0 -> result.secondaryValue to secondary
        else -> return null
    }
    return DoseLogRequest(productId, StoredDecimal.from(value), unit, volumeLitres)
}
