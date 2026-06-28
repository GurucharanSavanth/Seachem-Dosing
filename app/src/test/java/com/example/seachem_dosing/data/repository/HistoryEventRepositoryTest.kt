package com.example.seachem_dosing.data.repository

import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.data.local.dao.HistoryDao
import com.example.seachem_dosing.data.local.entity.DoseEventDetailEntity
import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.local.entity.ParameterEventDetailEntity
import com.example.seachem_dosing.domain.history.CorrectionCommand
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.ParameterValidationStatus
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RecordParameterCommand
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.history.VoidCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-011 Gate D — repository write boundary: validation, idempotency, atomic persistence. */
class HistoryEventRepositoryTest {

    private class FakeHistoryDao(var failOnInsert: Boolean = false) : HistoryDao() {
        val events = linkedMapOf<String, HistoryEventEntity>()
        val doseDetails = linkedMapOf<String, DoseEventDetailEntity>()
        val paramDetails = linkedMapOf<String, ParameterEventDetailEntity>()

        override suspend fun insertEvent(event: HistoryEventEntity) {
            if (failOnInsert) throw RuntimeException("simulated insert failure")
            require(events.values.none { it.idempotencyKey == event.idempotencyKey }) { "UNIQUE idempotency_key" }
            events[event.eventId] = event
        }
        override suspend fun insertDoseDetail(detail: DoseEventDetailEntity) { doseDetails[detail.eventId] = detail }
        override suspend fun insertParameterDetail(detail: ParameterEventDetailEntity) { paramDetails[detail.eventId] = detail }
        override suspend fun findEventIdByIdempotencyKey(key: String): String? =
            events.values.firstOrNull { it.idempotencyKey == key }?.eventId
        override suspend fun getEventById(id: String): HistoryEventEntity? = events[id]
        override fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>> = flowOf(rows(profileId))
        override fun observeByType(profileId: String, typeCode: String): Flow<List<HistoryTimelineRow>> =
            flowOf(rows(profileId).filter { it.event.eventTypeCode == typeCode })
        override fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long): Flow<List<HistoryTimelineRow>> =
            flowOf(rows(profileId).filter { it.event.occurredAtEpochMillis in fromInclusive..toInclusive })
        override fun observeVoidedEventIds(profileId: String): Flow<List<String>> =
            flowOf(events.values.filter { it.aquariumProfileId == profileId }.mapNotNull { it.voidsEventId })
        private fun rows(profileId: String) = events.values
            .filter { it.aquariumProfileId == profileId }
            .sortedByDescending { it.occurredAtEpochMillis }
            .map { HistoryTimelineRow(it, doseDetails[it.eventId], paramDetails[it.eventId]) }
    }

    private fun repo(dao: HistoryDao): HistoryEventRepositoryImpl {
        var n = 0
        return HistoryEventRepositoryImpl(dao) { "evt-${++n}" }
    }

    private fun q(v: String, u: UnitCode) = Quantity(StoredDecimal.parseNewValue(v), u)

    private fun dose(idem: String = "k1", occurredAt: Long = 1000, amount: String = "5.0") = RecordDoseCommand(
        idempotencyKey = idem, aquariumProfileId = "p1", sourceModuleCode = "calculator",
        occurredAtEpochMillis = occurredAt, createdAtEpochMillis = occurredAt, productId = "prime",
        administered = q(amount, UnitCode.MILLILITER), tankVolume = q("100", UnitCode.LITER),
        userModifiedAmount = false, appVersion = "1.0", engineVersion = "engine-1",
    )

    @Test fun recordDose_valid_returnsRecorded_andPersistsAtomically() = runTest {
        val dao = FakeHistoryDao()
        val outcome = repo(dao).recordDose(dose())
        assertTrue(outcome is HistoryWriteOutcome.Recorded)
        val id = (outcome as HistoryWriteOutcome.Recorded).eventId
        assertEquals(1, dao.events.size)
        assertEquals(1, dao.doseDetails.size)
        assertEquals(HistoryEventType.DOSE_ADMINISTERED.storageCode, dao.events[id]!!.eventTypeCode)
        assertEquals(2, dao.events[id]!!.schemaVersion)
    }

    @Test fun recordDose_decimalBoundary_canonicalPreserved_noRounding() = runTest {
        val dao = FakeHistoryDao()
        val out = repo(dao).recordDose(dose(amount = "5.00")) as HistoryWriteOutcome.Recorded
        // scale-preserving canonical string stored verbatim — no Double, no rounding.
        assertEquals("5.00", dao.doseDetails[out.eventId]!!.administeredAmountDecimal)
        assertEquals("100", dao.doseDetails[out.eventId]!!.tankVolumeDecimal)
    }

    @Test fun recordDose_duplicateIdempotencyKey_returnsDuplicate_singleRecord() = runTest {
        val dao = FakeHistoryDao()
        val r = repo(dao)
        val first = r.recordDose(dose("dup")) as HistoryWriteOutcome.Recorded
        val second = r.recordDose(dose("dup"))
        assertTrue(second is HistoryWriteOutcome.Duplicate)
        assertEquals(first.eventId, (second as HistoryWriteOutcome.Duplicate).eventId)
        assertEquals(1, dao.events.size)
    }

    @Test fun recordDose_invalid_returnsInvalid_noInsert() = runTest {
        val dao = FakeHistoryDao()
        val outcome = repo(dao).recordDose(dose().copy(productId = ""))
        assertTrue(outcome is HistoryWriteOutcome.Invalid)
        assertEquals(0, dao.events.size)
    }

    @Test fun recordDose_daoThrows_returnsFailed_noPartialEvent() = runTest {
        val dao = FakeHistoryDao(failOnInsert = true)
        val outcome = repo(dao).recordDose(dose())
        assertTrue(outcome is HistoryWriteOutcome.Failed)
        assertEquals(0, dao.events.size)
        assertEquals(0, dao.doseDetails.size)
    }

    @Test fun recordParameter_valid_persists() = runTest {
        val dao = FakeHistoryDao()
        val cmd = RecordParameterCommand(
            idempotencyKey = "pk", aquariumProfileId = "p1", sourceModuleCode = "dashboard",
            occurredAtEpochMillis = 1000, createdAtEpochMillis = 1000,
            parameterType = ParameterType.NITRATE, measured = q("15", UnitCode.PPM_MG_PER_L),
            validationStatus = ParameterValidationStatus.VALIDATED, appVersion = "1.0",
        )
        val out = repo(dao).recordParameter(cmd) as HistoryWriteOutcome.Recorded
        assertEquals("15", dao.paramDetails[out.eventId]!!.measuredValueDecimal)
        assertEquals(ParameterType.NITRATE.storageCode, dao.paramDetails[out.eventId]!!.parameterTypeCode)
    }

    @Test fun appendVoid_valid_persistsVoidEvent() = runTest {
        val dao = FakeHistoryDao()
        val out = repo(dao).appendVoid(VoidCommand("vk", "p1", "history", 1000, 1000, voidsEventId = "evt-target", reason = "dup"))
            as HistoryWriteOutcome.Recorded
        assertEquals("evt-target", dao.events[out.eventId]!!.voidsEventId)
        assertEquals(HistoryEventType.VOID.storageCode, dao.events[out.eventId]!!.eventTypeCode)
    }

    @Test fun appendCorrection_withReplacement_persistsHeaderAndDetail() = runTest {
        val dao = FakeHistoryDao()
        val cmd = CorrectionCommand("ck", "p1", "history", 1000, 1000, supersedesEventId = "evt-orig", reason = "wrong amount",
            replacement = dose(idem = "ignored", amount = "3.5"))
        val out = repo(dao).appendCorrection(cmd) as HistoryWriteOutcome.Recorded
        assertEquals("evt-orig", dao.events[out.eventId]!!.supersedesEventId)
        assertEquals("wrong amount", dao.events[out.eventId]!!.correctionReason)
        assertEquals("3.5", dao.doseDetails[out.eventId]!!.administeredAmountDecimal)
    }

    @Test fun observeTimeline_reflectsInserts_newestFirst() = runTest {
        val dao = FakeHistoryDao()
        val r = repo(dao)
        r.recordDose(dose(idem = "a", occurredAt = 1000))
        r.recordDose(dose(idem = "b", occurredAt = 2000))
        val rows = r.observeTimeline("p1").first()
        assertEquals(2, rows.size)
        assertEquals(2000, rows.first().event.occurredAtEpochMillis) // newest first
    }
}
