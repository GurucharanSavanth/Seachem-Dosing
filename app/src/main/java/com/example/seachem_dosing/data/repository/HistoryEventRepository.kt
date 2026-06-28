package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.data.local.dao.HistoryDao
import com.example.seachem_dosing.data.local.entity.DoseEventDetailEntity
import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.local.entity.ParameterEventDetailEntity
import com.example.seachem_dosing.domain.history.CorrectionCommand
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.HistoryWriteValidator
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RecordParameterCommand
import com.example.seachem_dosing.domain.history.VoidCommand
import kotlinx.coroutines.flow.Flow

/** Outcome of a history write. Idempotent replays return [Duplicate]; invalid input never inserts. */
sealed interface HistoryWriteOutcome {
    data class Recorded(val eventId: String) : HistoryWriteOutcome
    data class Duplicate(val eventId: String) : HistoryWriteOutcome
    data class Invalid(val violations: List<String>) : HistoryWriteOutcome
    data class Failed(val message: String) : HistoryWriteOutcome
}

/**
 * Audit-safe write + read boundary over [HistoryDao] (ADR-011 Gate D). All writes go through
 * [HistoryWriteValidator]; commands carry validated `StoredDecimal`/registry types, so a raw
 * decimal string or unknown unit cannot reach persistence. Append-only — no physical delete.
 */
interface HistoryEventRepository {
    suspend fun recordDose(cmd: RecordDoseCommand): HistoryWriteOutcome
    suspend fun recordParameter(cmd: RecordParameterCommand): HistoryWriteOutcome
    suspend fun appendCorrection(cmd: CorrectionCommand): HistoryWriteOutcome
    suspend fun appendVoid(cmd: VoidCommand): HistoryWriteOutcome

    fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>>
    fun observeByType(profileId: String, type: HistoryEventType): Flow<List<HistoryTimelineRow>>
    fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long): Flow<List<HistoryTimelineRow>>
    fun observeVoidedEventIds(profileId: String): Flow<List<String>>
}

/** @param newEventId injected id source (production: UUID; tests: deterministic) for replayable writes. */
class HistoryEventRepositoryImpl(
    private val dao: HistoryDao,
    private val newEventId: () -> String,
) : HistoryEventRepository {

    override suspend fun recordDose(cmd: RecordDoseCommand): HistoryWriteOutcome =
        write(cmd.idempotencyKey, HistoryWriteValidator.validate(cmd)) { eventId ->
            dao.insertDoseEvent(
                event = eventHeader(eventId, cmd.idempotencyKey, HistoryEventType.DOSE_ADMINISTERED, cmd.aquariumProfileId,
                    cmd.sourceModuleCode, cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis,
                    cmd.precisionStatus.storageCode, cmd.appVersion, cmd.engineVersion, cmd.notes),
                detail = doseDetail(eventId, cmd),
            )
        }

    override suspend fun recordParameter(cmd: RecordParameterCommand): HistoryWriteOutcome =
        write(cmd.idempotencyKey, HistoryWriteValidator.validate(cmd)) { eventId ->
            dao.insertParameterEvent(
                event = eventHeader(eventId, cmd.idempotencyKey, HistoryEventType.WATER_PARAMETER_RECORDED, cmd.aquariumProfileId,
                    cmd.sourceModuleCode, cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis,
                    cmd.precisionStatus.storageCode, cmd.appVersion, engineVersion = null, notes = cmd.notes),
                detail = ParameterEventDetailEntity(
                    eventId = eventId,
                    parameterTypeCode = cmd.parameterType.storageCode,
                    measuredValueDecimal = cmd.measured.value.canonicalValue,
                    measuredUnitCode = cmd.measured.unit.storageCode,
                    testMethod = cmd.testMethod,
                    sourceDeviceOrKit = cmd.sourceDeviceOrKit,
                    validationStatusCode = cmd.validationStatus.storageCode,
                ),
            )
        }

    override suspend fun appendCorrection(cmd: CorrectionCommand): HistoryWriteOutcome =
        write(cmd.idempotencyKey, HistoryWriteValidator.validate(cmd)) { eventId ->
            dao.insertCorrectionEvent(
                event = eventHeader(eventId, cmd.idempotencyKey, HistoryEventType.CORRECTION, cmd.aquariumProfileId,
                    cmd.sourceModuleCode, cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis,
                    precisionStatusCode = cmd.replacement?.precisionStatus?.storageCode
                        ?: com.example.seachem_dosing.domain.history.PrecisionStatus.UNKNOWN_PRECISION.storageCode,
                    appVersion = cmd.replacement?.appVersion, engineVersion = cmd.replacement?.engineVersion,
                    notes = cmd.notes).copy(supersedesEventId = cmd.supersedesEventId, correctionReason = cmd.reason),
                replacementDose = cmd.replacement?.let { doseDetail(eventId, it) },
            )
        }

    override suspend fun appendVoid(cmd: VoidCommand): HistoryWriteOutcome =
        write(cmd.idempotencyKey, HistoryWriteValidator.validate(cmd)) { eventId ->
            dao.insertVoidEvent(
                eventHeader(eventId, cmd.idempotencyKey, HistoryEventType.VOID, cmd.aquariumProfileId,
                    cmd.sourceModuleCode, cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis,
                    com.example.seachem_dosing.domain.history.PrecisionStatus.UNKNOWN_PRECISION.storageCode,
                    appVersion = null, engineVersion = null, notes = cmd.notes)
                    .copy(voidsEventId = cmd.voidsEventId, correctionReason = cmd.reason),
            )
        }

    override fun observeTimeline(profileId: String) = dao.observeTimeline(profileId)
    override fun observeByType(profileId: String, type: HistoryEventType) = dao.observeByType(profileId, type.storageCode)
    override fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long) =
        dao.observeByDateRange(profileId, fromInclusive, toInclusive)
    override fun observeVoidedEventIds(profileId: String) = dao.observeVoidedEventIds(profileId)

    /** Shared write pipeline: reject invalid; return existing on idempotency hit; else insert. */
    private suspend inline fun write(
        idempotencyKey: String,
        violations: List<String>,
        insert: (eventId: String) -> Unit,
    ): HistoryWriteOutcome {
        if (violations.isNotEmpty()) return HistoryWriteOutcome.Invalid(violations)
        dao.findEventIdByIdempotencyKey(idempotencyKey)?.let { return HistoryWriteOutcome.Duplicate(it) }
        val eventId = newEventId()
        return try {
            insert(eventId)
            HistoryWriteOutcome.Recorded(eventId)
        } catch (e: Exception) {
            // race: another writer may have inserted the same idempotency key meanwhile
            dao.findEventIdByIdempotencyKey(idempotencyKey)?.let { return HistoryWriteOutcome.Duplicate(it) }
            HistoryWriteOutcome.Failed(e.message ?: "history insert failed")
        }
    }

    @Suppress("LongParameterList")
    private fun eventHeader(
        eventId: String, idempotencyKey: String, type: HistoryEventType, profileId: String,
        sourceModuleCode: String, occurredAt: Long, createdAt: Long, precisionStatusCode: String,
        appVersion: String?, engineVersion: String?, notes: String?,
    ) = HistoryEventEntity(
        eventId = eventId,
        eventTypeCode = type.storageCode,
        aquariumProfileId = profileId,
        occurredAtEpochMillis = occurredAt,
        createdAtEpochMillis = createdAt,
        sourceModuleCode = sourceModuleCode,
        appVersion = appVersion,
        engineVersion = engineVersion,
        idempotencyKey = idempotencyKey,
        schemaVersion = SCHEMA_VERSION,
        precisionStatusCode = precisionStatusCode,
        notes = notes,
    )

    private fun doseDetail(eventId: String, cmd: RecordDoseCommand) = DoseEventDetailEntity(
        eventId = eventId,
        productId = cmd.productId,
        productVariantId = cmd.productVariantId,
        formulaRuleId = cmd.formulaRuleId,
        evidenceSourceId = cmd.evidenceSourceId,
        routeCode = cmd.route?.storageCode,
        concentrationDecimal = cmd.concentration?.value?.canonicalValue,
        concentrationUnitCode = cmd.concentration?.unit?.storageCode,
        tankVolumeDecimal = cmd.tankVolume.value.canonicalValue,
        tankVolumeUnitCode = cmd.tankVolume.unit.storageCode,
        calculatedAmountDecimal = cmd.calculated?.value?.canonicalValue,
        calculatedAmountUnitCode = cmd.calculated?.unit?.storageCode,
        administeredAmountDecimal = cmd.administered.value.canonicalValue,
        administeredAmountUnitCode = cmd.administered.unit.storageCode,
        roundingModeCode = cmd.rounding?.modeCode,
        roundingScale = cmd.rounding?.scale,
        userModifiedAmount = cmd.userModifiedAmount,
        warningsAcknowledged = cmd.warningsAcknowledged,
        administeredScoopDefinitionId = cmd.administeredCalibration?.scoopDefinitionId,
        administeredCalibratedVolumeDecimal = cmd.administeredCalibration?.calibratedVolume?.value?.canonicalValue,
        administeredCalibratedVolumeUnitCode = cmd.administeredCalibration?.calibratedVolume?.unit?.storageCode,
    )

    companion object {
        const val SCHEMA_VERSION = 2
    }
}
