package com.example.seachem_dosing.ui.dashboard

import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.CorrectionCommand
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RecordParameterCommand
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.history.VoidCommand
import com.example.seachem_dosing.domain.usecase.RecordWaterParameterReadingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WaterReadingsRecorderTest {

    private class CapturingRepo : HistoryEventRepository {
        val params = mutableListOf<RecordParameterCommand>()
        override suspend fun recordParameter(cmd: RecordParameterCommand): HistoryWriteOutcome {
            params += cmd; return HistoryWriteOutcome.Recorded("e")
        }
        override suspend fun recordDose(cmd: RecordDoseCommand) = HistoryWriteOutcome.Recorded("e")
        override suspend fun appendCorrection(cmd: CorrectionCommand) = HistoryWriteOutcome.Recorded("e")
        override suspend fun appendVoid(cmd: VoidCommand) = HistoryWriteOutcome.Recorded("e")
        override fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>> = emptyFlow()
        override fun observeByType(profileId: String, type: HistoryEventType): Flow<List<HistoryTimelineRow>> = emptyFlow()
        override fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long) = emptyFlow<List<HistoryTimelineRow>>()
        override fun observeVoidedEventIds(profileId: String): Flow<List<String>> = emptyFlow()
    }

    @Test fun save_recordsEachReading_withCorrectUnitsVolumeAndIdempotency() = runTest {
        val repo = CapturingRepo()
        val recorder = WaterReadingsRecorder(RecordWaterParameterReadingUseCase(repo, { 1000L }, "1.0"))

        val saved = recorder.save(
            profileId = "fw", volumeLitres = 100.0, batchKey = "batch",
            readings = listOf(
                ParameterType.NITRATE to 15.0,
                ParameterType.PH to 7.4,
                ParameterType.TEMPERATURE to 26.0,
            ),
        )

        assertEquals(3, saved)
        assertEquals(3, repo.params.size)
        val nitrate = repo.params.first { it.parameterType == ParameterType.NITRATE }
        assertEquals("15.0", nitrate.measured.value.canonicalValue)
        assertEquals(UnitCode.PPM_MG_PER_L, nitrate.measured.unit)
        assertEquals("100.0", nitrate.tankVolume?.value?.canonicalValue)
        assertEquals(UnitCode.LITER, nitrate.tankVolume?.unit)
        assertEquals("batch:nitrate", nitrate.idempotencyKey) // idempotency keyed per batch+param
        assertEquals(UnitCode.PH_VALUE, repo.params.first { it.parameterType == ParameterType.PH }.measured.unit)
        assertEquals(UnitCode.CELSIUS, repo.params.first { it.parameterType == ParameterType.TEMPERATURE }.measured.unit)
    }
}
