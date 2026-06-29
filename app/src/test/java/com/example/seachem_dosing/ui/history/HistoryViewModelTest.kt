package com.example.seachem_dosing.ui.history

import com.example.seachem_dosing.data.local.entity.HistoryEventEntity
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.CorrectionCommand
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.RecordDoseCommand
import com.example.seachem_dosing.domain.history.RecordParameterCommand
import com.example.seachem_dosing.domain.history.VoidCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(StandardTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun row(id: String) = HistoryTimelineRow(
        HistoryEventEntity(
            eventId = id, eventTypeCode = "dose_administered", aquariumProfileId = "fw",
            occurredAtEpochMillis = 1000, createdAtEpochMillis = 1000, sourceModuleCode = "calc",
            idempotencyKey = id, schemaVersion = 2, precisionStatusCode = "new_exact",
        ),
        dose = null, parameter = null,
    )

    private class FakeRepo(
        val timeline: List<HistoryTimelineRow> = emptyList(),
        val byType: List<HistoryTimelineRow> = emptyList(),
        val voided: List<String> = emptyList(),
    ) : HistoryEventRepository {
        var lastTypeQueried: HistoryEventType? = null
        override suspend fun recordDose(cmd: RecordDoseCommand) = HistoryWriteOutcome.Recorded("e")
        override suspend fun recordParameter(cmd: RecordParameterCommand) = HistoryWriteOutcome.Recorded("e")
        override suspend fun appendCorrection(cmd: CorrectionCommand) = HistoryWriteOutcome.Recorded("e")
        override suspend fun appendVoid(cmd: VoidCommand) = HistoryWriteOutcome.Recorded("e")
        override fun observeTimeline(profileId: String): Flow<List<HistoryTimelineRow>> = flowOf(timeline)
        override fun observeByType(profileId: String, type: HistoryEventType): Flow<List<HistoryTimelineRow>> {
            lastTypeQueried = type
            return flowOf(byType)
        }
        override fun observeByDateRange(profileId: String, fromInclusive: Long, toInclusive: Long) = flowOf(emptyList<HistoryTimelineRow>())
        override fun observeVoidedEventIds(profileId: String): Flow<List<String>> = flowOf(voided)
    }

    @Test fun withRows_emitsContent_withVoidedSet() = runTest {
        val vm = HistoryViewModel(FakeRepo(timeline = listOf(row("a"), row("b")), voided = listOf("a")))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.setProfile("fw")
        advanceUntilIdle()
        val s = vm.uiState.value
        assertTrue("expected Content, got $s", s is HistoryUiState.Content)
        s as HistoryUiState.Content
        assertEquals(2, s.rows.size)
        assertEquals(setOf("a"), s.voidedEventIds)
    }

    @Test fun noRows_emitsEmpty() = runTest {
        val vm = HistoryViewModel(FakeRepo(timeline = emptyList()))
        backgroundScope.launch { vm.uiState.collect {} }
        vm.setProfile("fw")
        advanceUntilIdle()
        assertTrue(vm.uiState.value is HistoryUiState.Empty)
    }

    @Test fun typeFilter_routesToObserveByType() = runTest {
        val repo = FakeRepo(byType = listOf(row("d")))
        val vm = HistoryViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        vm.setProfile("fw")
        vm.setTypeFilter(HistoryEventType.DOSE_ADMINISTERED)
        advanceUntilIdle()
        assertEquals(HistoryEventType.DOSE_ADMINISTERED, repo.lastTypeQueried)
        assertTrue(vm.uiState.value is HistoryUiState.Content)
    }

    @Test fun noProfileYet_staysLoading() = runTest {
        val vm = HistoryViewModel(FakeRepo())
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value is HistoryUiState.Loading)
    }
}
