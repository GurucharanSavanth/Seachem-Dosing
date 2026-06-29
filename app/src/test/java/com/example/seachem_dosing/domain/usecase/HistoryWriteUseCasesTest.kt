package com.example.seachem_dosing.domain.usecase

import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.CorrectionCommand
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.PrecisionStatus
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RecordParameterCommand
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.history.VoidCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class CapturingRepo : HistoryEventRepository {
    var lastDose: RecordDoseCommand? = null
    var lastParam: RecordParameterCommand? = null
    override suspend fun recordDose(cmd: RecordDoseCommand): HistoryWriteOutcome {
        lastDose = cmd; return HistoryWriteOutcome.Recorded("e")
    }
    override suspend fun recordParameter(cmd: RecordParameterCommand): HistoryWriteOutcome {
        lastParam = cmd; return HistoryWriteOutcome.Recorded("e")
    }
    override suspend fun appendCorrection(cmd: CorrectionCommand) = HistoryWriteOutcome.Recorded("e")
    override suspend fun appendVoid(cmd: VoidCommand) = HistoryWriteOutcome.Recorded("e")
    override fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>> = emptyFlow()
    override fun observeByType(profileId: String, type: HistoryEventType): Flow<List<HistoryTimelineRow>> = emptyFlow()
    override fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long): Flow<List<HistoryTimelineRow>> = emptyFlow()
    override fun observeVoidedEventIds(profileId: String): Flow<List<String>> = emptyFlow()
}

class HistoryWriteUseCasesTest {

    private fun q(v: String, u: UnitCode) = Quantity(StoredDecimal.parseNewValue(v), u)

    @Test fun logAdministeredDose_buildsNewExactDoseCommand_withInjectedClockAndVersions() = runTest {
        val repo = CapturingRepo()
        LogAdministeredDoseUseCase(repo, now = { 5000L }, appVersion = "1.0").invoke(
            idempotencyKey = "k", aquariumProfileId = "fw", productId = "prime",
            administered = q("5.0", UnitCode.MILLILITER), tankVolume = q("100", UnitCode.LITER),
            engineVersion = "engine-1", userModifiedAmount = true, calculated = q("4.0", UnitCode.MILLILITER),
        )
        val cmd = repo.lastDose!!
        assertEquals(5000L, cmd.occurredAtEpochMillis)
        assertEquals(5000L, cmd.createdAtEpochMillis)
        assertEquals("calculator", cmd.sourceModuleCode)
        assertEquals("1.0", cmd.appVersion)
        assertEquals("engine-1", cmd.engineVersion)
        assertEquals(PrecisionStatus.NEW_EXACT_RECORD, cmd.precisionStatus)
        assertTrue(cmd.userModifiedAmount)
        assertEquals("5.0", cmd.administered.value.canonicalValue)
        assertEquals("4.0", cmd.calculated?.value?.canonicalValue) // calculated preserved separately
    }

    @Test fun recordParameterReading_buildsNewExactParameterCommand_dashboardSource() = runTest {
        val repo = CapturingRepo()
        RecordWaterParameterReadingUseCase(repo, now = { 9000L }, appVersion = "1.0").invoke(
            idempotencyKey = "p", aquariumProfileId = "fw", parameterType = ParameterType.NITRATE,
            measured = q("15", UnitCode.PPM_MG_PER_L), tankVolume = q("100", UnitCode.LITER),
        )
        val cmd = repo.lastParam!!
        assertEquals(9000L, cmd.occurredAtEpochMillis)
        assertEquals("dashboard", cmd.sourceModuleCode)
        assertEquals("1.0", cmd.appVersion)
        assertEquals(PrecisionStatus.NEW_EXACT_RECORD, cmd.precisionStatus)
        assertEquals(ParameterType.NITRATE, cmd.parameterType)
        assertEquals("100", cmd.tankVolume?.value?.canonicalValue)
    }
}
