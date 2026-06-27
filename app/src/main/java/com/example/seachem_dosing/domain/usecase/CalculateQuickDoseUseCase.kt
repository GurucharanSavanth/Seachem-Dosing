package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.repository.CalculationsRepository

/**
 * Volume-only "quick" dose calculations (Prime, Stability, Safe, APT Complete).
 * No current/target inputs — these doses depend only on tank volume.
 */
class CalculateQuickDoseUseCase(
    private val calculationsRepository: CalculationsRepository
) {
    fun prime(volumeLitres: Double): Double =
        calculationsRepository.calculatePrimeDose(volumeLitres)

    fun stability(volumeLitres: Double): Double =
        calculationsRepository.calculateStabilityDose(volumeLitres)

    fun safe(volumeLitres: Double): Double =
        calculationsRepository.calculateSafe(volumeLitres)

    fun aptComplete(
        volumeLitres: Double,
        currentNitrate: Double
    ): CalculationsRepository.AptResult =
        calculationsRepository.calculateAptComplete(volumeLitres, currentNitrate)
}
