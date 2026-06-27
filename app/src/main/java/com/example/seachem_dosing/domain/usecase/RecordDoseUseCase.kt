package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.local.entity.DosingLogEntity
import com.example.seachem_dosing.data.repository.HistoryRepository

/**
 * Persists a dosing event (computed and/or administered) to the dosing_log table.
 * Returns the row id of the inserted record.
 *
 * Volume snapshot is required (caller passes current effective volume) so
 * downstream analytics remain correct even if the user later changes their
 * configured tank volume.
 */
class RecordDoseUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(
        profileId: String,
        product: String,
        amount: Double,
        unit: String,
        volumeLitresAtDose: Double,
        administered: Boolean = false,
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): Long {
        return historyRepository.recordDose(
            DosingLogEntity(
                profileId = profileId,
                timestamp = timestamp,
                product = product,
                amount = amount,
                unit = unit,
                volumeLitresAtDose = volumeLitresAtDose,
                administered = administered,
                notes = notes
            )
        )
    }
}
