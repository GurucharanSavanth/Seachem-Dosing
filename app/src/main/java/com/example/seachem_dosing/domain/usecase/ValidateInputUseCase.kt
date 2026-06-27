package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.domain.model.ValidationResult

/**
 * Validates dosing-calculation inputs. Returns a structured
 * [ValidationResult] so the UI can surface field-specific errors instead
 * of a generic "invalid" toast.
 *
 * Range constants mirror the coercions in v1.0
 * [com.example.seachem_dosing.ui.MainViewModel] (`coercePh`, `coerceTemperature`,
 * etc.) so behaviour stays consistent through the migration.
 */
class ValidateInputUseCase {

    fun validatePh(value: Double): ValidationResult = when {
        value < PH_MIN -> ValidationResult.Invalid("ph", "pH must be ≥ $PH_MIN")
        value > PH_MAX -> ValidationResult.Invalid("ph", "pH must be ≤ $PH_MAX")
        else -> ValidationResult.Valid
    }

    fun validateNonNegative(field: String, value: Double): ValidationResult =
        if (value < 0.0) ValidationResult.Invalid(field, "$field must be non-negative")
        else ValidationResult.Valid

    fun validatePurity(value: Double): ValidationResult = when {
        value < PURITY_MIN -> ValidationResult.Invalid("purity", "Purity must be ≥ $PURITY_MIN")
        value > PURITY_MAX -> ValidationResult.Invalid("purity", "Purity must be ≤ $PURITY_MAX")
        else -> ValidationResult.Valid
    }

    fun validateVolumeLitres(value: Double): ValidationResult =
        if (value <= 0.0) ValidationResult.Invalid("volume", "Volume must be > 0")
        else ValidationResult.Valid

    fun validateTemperatureCelsius(value: Double): ValidationResult = when {
        value < TEMP_MIN -> ValidationResult.Invalid("temperature", "Temperature must be ≥ $TEMP_MIN °C")
        value > TEMP_MAX -> ValidationResult.Invalid("temperature", "Temperature must be ≤ $TEMP_MAX °C")
        else -> ValidationResult.Valid
    }

    fun validateSalinityPpt(value: Double): ValidationResult = when {
        value < SALINITY_MIN -> ValidationResult.Invalid("salinity", "Salinity must be ≥ $SALINITY_MIN PPT")
        value > SALINITY_MAX -> ValidationResult.Invalid("salinity", "Salinity must be ≤ $SALINITY_MAX PPT")
        else -> ValidationResult.Valid
    }

    private companion object {
        const val PH_MIN = 0.0
        const val PH_MAX = 14.0
        const val PURITY_MIN = 0.5
        const val PURITY_MAX = 1.0
        const val TEMP_MIN = -5.0
        const val TEMP_MAX = 50.0
        const val SALINITY_MIN = 0.0
        const val SALINITY_MAX = 50.0
    }
}
