package com.example.seachem_dosing.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.theme.SeachemTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Hosts the Compose [HistoryScreen] (ADR-008 read-side). [HistoryViewModel] (Koin) drives the
 * timeline; the active aquarium profile comes from the shared [MainViewModel]. The type filter is
 * `rememberSaveable` so it survives process death and is re-applied to the VM.
 */
class HistoryFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private val historyViewModel: HistoryViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SeachemTheme {
                val state by historyViewModel.uiState.collectAsStateWithLifecycle()
                val profile by mainViewModel.profile.observeAsState()
                var selectedCode by rememberSaveable { mutableStateOf<String?>(null) }
                val selectedType = selectedCode?.let { HistoryEventType.fromCode(it) }

                LaunchedEffect(profile) { profile?.let { historyViewModel.setProfile(it.id) } }
                LaunchedEffect(selectedType) { historyViewModel.setTypeFilter(selectedType) }

                HistoryScreen(
                    state = state,
                    selectedType = selectedType,
                    onSelectType = { selectedCode = it?.storageCode },
                    onRetry = historyViewModel::retry,
                )
            }
        }
    }
}
