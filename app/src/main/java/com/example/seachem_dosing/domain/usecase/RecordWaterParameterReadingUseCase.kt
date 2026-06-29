package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.ParameterValidationStatus
import com.example.seachem_dosing.domain.history.PrecisionStatus
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.RecordParameterCommand

/**
 * Records an **explicitly user-confirmed** water-parameter reading to the history audit log
 * (ADR-011 §11). Never auto-triggered — only from a "Save reading" action. A reading is measured,
 * not engine-computed, so no engine version is required. [idempotencyKey] is generated once per
 * action so repeated taps produce a single record.
 *
 * @param now injected clock (testable). @param appVersion BuildConfig.VERSION_NAME.
 */
class RecordWaterParameterReadingUseCase(
    private val repository: HistoryEventRepository,
    private val now: () -> Long,
    private val appVersion: String,
) {
    suspend operator fun invoke(
        idempotencyKey: String,
        aquariumProfileId: String,
        parameterType: ParameterType,
        measured: Quantity,
        validationStatus: ParameterValidationStatus = ParameterValidationStatus.VALIDATED,
        tankVolume: Quantity? = null,
        testMethod: String? = null,
        sourceDeviceOrKit: String? = null,
        notes: String? = null,
    ): HistoryWriteOutcome {
        val timestamp = now()
        return repository.recordParameter(
            RecordParameterCommand(
                idempotencyKey = idempotencyKey,
                aquariumProfileId = aquariumProfileId,
                sourceModuleCode = "dashboard",
                occurredAtEpochMillis = timestamp,
                createdAtEpochMillis = timestamp,
                parameterType = parameterType,
                measured = measured,
                validationStatus = validationStatus,
                precisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
                tankVolume = tankVolume,
                testMethod = testMethod,
                sourceDeviceOrKit = sourceDeviceOrKit,
                appVersion = appVersion,
                notes = notes,
            ),
        )
    }
}
