package com.example.seachem_dosing.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.data.repository.HistoryEventRepository
import com.example.seachem_dosing.domain.history.HistoryEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** Immutable UI state for the History timeline (ADR-011 §11; ADR-008 read-side). */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Empty(val typeFilter: HistoryEventType?) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
    data class Content(
        val rows: List<HistoryTimelineRow>,
        val voidedEventIds: Set<String>,
        val typeFilter: HistoryEventType?,
    ) : HistoryUiState
}

/**
 * Drives the History screen. Combines the active profile + type filter into a single timeline flow
 * (newest-first, profile-scoped) plus the set of voided event ids so the UI can flag retracted
 * records. Errors are surfaced as [HistoryUiState.Error] with a retry; append-only — no delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: HistoryEventRepository,
) : ViewModel() {

    private val profileId = MutableStateFlow<String?>(null)
    private val typeFilter = MutableStateFlow<HistoryEventType?>(null)
    private val retryTick = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> =
        combine(profileId, typeFilter, retryTick) { profile, type, _ -> profile to type }
            .flatMapLatest { (profile, type) ->
                if (profile == null) {
                    flowOf(HistoryUiState.Loading)
                } else {
                    val rows: Flow<List<HistoryTimelineRow>> =
                        if (type == null) repository.observeTimeline(profile)
                        else repository.observeByType(profile, type)
                    combine(rows, repository.observeVoidedEventIds(profile)) { list, voided ->
                        if (list.isEmpty()) {
                            HistoryUiState.Empty(type)
                        } else {
                            HistoryUiState.Content(list, voided.toSet(), type)
                        }
                    }.catch { emit(HistoryUiState.Error(it.message ?: "Could not load history")) }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    /** Called by the host when the active aquarium profile changes. */
    fun setProfile(id: String) { profileId.value = id }

    /** null = all event types. */
    fun setTypeFilter(type: HistoryEventType?) { typeFilter.value = type }

    fun retry() { retryTick.value += 1 }
}
