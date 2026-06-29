package com.example.seachem_dosing.ui.dashboard

import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.LegacyParameterMapping
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.usecase.RecordWaterParameterReadingUseCase
import java.math.BigDecimal

/**
 * Records the current dashboard water-parameter readings to history via
 * [RecordWaterParameterReadingUseCase] (ADR-011 §11). Each reading is idempotency-keyed by
 * [batchKey] + parameter so a double-tap or recomposition produces one record per parameter.
 *
 * The dashboard holds parameters as `Double`; values are canonicalized via
 * `BigDecimal.valueOf(value)` (shortest-decimal repr, not the binary-exact ctor). Units follow the
 * app's default convention ([LegacyParameterMapping]); tank volume in litres.
 */
class WaterReadingsRecorder(
    private val recordReading: RecordWaterParameterReadingUseCase,
) {
    suspend fun save(
        profileId: String,
        volumeLitres: Double,
        batchKey: String,
        readings: List<Pair<ParameterType, Double>>,
    ): Int {
        var saved = 0
        val volume = Quantity(StoredDecimal.from(BigDecimal.valueOf(volumeLitres)), UnitCode.LITER)
        for ((type, value) in readings) {
            val outcome = recordReading(
                idempotencyKey = "$batchKey:${type.storageCode}",
                aquariumProfileId = profileId,
                parameterType = type,
                measured = Quantity(StoredDecimal.from(BigDecimal.valueOf(value)), LegacyParameterMapping.unitFor(type)),
                tankVolume = volume,
            )
            if (outcome is HistoryWriteOutcome.Recorded || outcome is HistoryWriteOutcome.Duplicate) saved++
        }
        return saved
    }
}
