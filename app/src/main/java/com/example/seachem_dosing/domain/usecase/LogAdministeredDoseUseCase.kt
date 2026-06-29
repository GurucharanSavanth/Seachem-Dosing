package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.AdministrationRoute
import com.example.seachem_dosing.domain.history.PrecisionStatus
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RoundingInfo

/**
 * Records an **explicitly user-confirmed** administered dose to the history audit log (ADR-011 §11).
 * Never called merely because a calculation was displayed — only from a "Log as administered" action.
 *
 * Calculated and confirmed [administered] amounts are kept separate; [userModifiedAmount] flags when
 * the user changed the calculated value. [idempotencyKey] is generated once per action by the caller
 * so repeated taps / recompositions / process recreation produce a single record.
 *
 * @param now injected clock (testable). @param appVersion BuildConfig.VERSION_NAME.
 */
class LogAdministeredDoseUseCase(
    private val repository: HistoryEventRepository,
    private val now: () -> Long,
    private val appVersion: String,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        idempotencyKey: String,
        aquariumProfileId: String,
        productId: String,
        administered: Quantity,
        tankVolume: Quantity,
        engineVersion: String,
        userModifiedAmount: Boolean,
        calculated: Quantity? = null,
        calculatedMeasureDefinitionId: String? = null,
        administeredMeasureDefinitionId: String? = null,
        concentration: Quantity? = null,
        route: AdministrationRoute? = null,
        rounding: RoundingInfo? = null,
        formulaRuleId: String? = null,
        evidenceSourceId: String? = null,
        warningsAcknowledged: String? = null,
        notes: String? = null,
    ): HistoryWriteOutcome {
        val timestamp = now()
        return repository.recordDose(
            RecordDoseCommand(
                idempotencyKey = idempotencyKey,
                aquariumProfileId = aquariumProfileId,
                sourceModuleCode = "calculator",
                occurredAtEpochMillis = timestamp,
                createdAtEpochMillis = timestamp,
                productId = productId,
                administered = administered,
                tankVolume = tankVolume,
                userModifiedAmount = userModifiedAmount,
                precisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
                calculated = calculated,
                calculatedMeasureDefinitionId = calculatedMeasureDefinitionId,
                administeredMeasureDefinitionId = administeredMeasureDefinitionId,
                concentration = concentration,
                route = route,
                rounding = rounding,
                formulaRuleId = formulaRuleId,
                evidenceSourceId = evidenceSourceId,
                warningsAcknowledged = warningsAcknowledged,
                appVersion = appVersion,
                engineVersion = engineVersion,
                notes = notes,
            ),
        )
    }
}
