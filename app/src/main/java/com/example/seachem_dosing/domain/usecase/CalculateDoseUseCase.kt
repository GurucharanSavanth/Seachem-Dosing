package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.repository.CalculationsRepository
import com.example.seachem_dosing.domain.model.DosingResult
import com.example.seachem_dosing.logic.SeachemCalculations
import java.math.BigDecimal

/**
 * Single-shot dose calculation for products in [SeachemCalculations.Product].
 *
 * Validates volume > 0 before delegating to [CalculationsRepository]. Future
 * cross-cutting concerns (audit logging, safety interlocks, dose caps) hook
 * in here without touching the calculation engine.
 */
class CalculateDoseUseCase(
    private val calculationsRepository: CalculationsRepository
) {
    operator fun invoke(
        product: SeachemCalculations.Product,
        currentValue: BigDecimal,
        targetValue: BigDecimal,
        volumeLitres: BigDecimal,
        scale: SeachemCalculations.UnitScale = SeachemCalculations.UnitScale.PPM
    ): DosingResult {
        if (volumeLitres <= BigDecimal.ZERO) {
            return DosingResult.Error(
                code = "ZERO_VOLUME",
                message = "Tank volume must be greater than zero.",
                recoverable = true
            )
        }
        return calculationsRepository.calculateForProduct(
            product = product,
            currentValue = currentValue,
            targetValue = targetValue,
            volumeLitres = volumeLitres,
            scale = scale
        )
    }
}
