package com.example.seachem_dosing.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.theme.SeachemTheme
import androidx.lifecycle.lifecycleScope
import com.example.seachem_dosing.domain.history.ParameterType
import com.example.seachem_dosing.domain.usecase.RecordWaterParameterReadingUseCase
import com.google.android.material.transition.MaterialFadeThrough
import java.util.UUID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Hosts the Compose [DashboardScreen] (ADR-001). All volume/parameter wiring and
 * the recommendation rendering moved into the composable + [com.example.seachem_dosing.domain.engine.RecommendationEngine];
 * only clipboard copy + share (which need a Context/Activity) stay here as callbacks.
 */
class DashboardFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val recordReading: RecordWaterParameterReadingUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            SeachemTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onCopy = ::copyRecommendations,
                    onShare = ::shareRecommendations,
                    onSaveReadings = ::saveReadings,
                )
            }
        }
    }

    private fun saveReadings() {
        val profileId = (viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER).id
        val volume = viewModel.getEffectiveVolumeLitres()
        val readings = buildReadings()
        if (readings.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.save_readings_none), Toast.LENGTH_SHORT).show()
            return
        }
        val batchKey = "dashboard:${UUID.randomUUID()}"
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = WaterReadingsRecorder(recordReading).save(profileId, volume, batchKey, readings)
            Toast.makeText(
                requireContext(),
                resources.getQuantityString(R.plurals.save_readings_done, saved, saved),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun buildReadings(): List<Pair<ParameterType, Double>> {
        val vm = viewModel
        val common = listOf(
            ParameterType.NITRATE to (vm.nitrate.value ?: 0.0),
            ParameterType.PH to (vm.ph.value ?: 0.0),
            ParameterType.TEMPERATURE to (vm.temperature.value ?: 0.0),
            ParameterType.GH to vm.getGhInDegrees(),
            ParameterType.KH to vm.getKhInDegrees(),
        )
        val saltwater = if (vm.profile.value == MainViewModel.AquariumProfile.SALTWATER) {
            listOf(
                ParameterType.SALINITY to (vm.salinity.value ?: 0.0),
                ParameterType.ALKALINITY to (vm.alkalinity.value ?: 0.0),
                ParameterType.CALCIUM to (vm.calcium.value ?: 0.0),
                ParameterType.MAGNESIUM to (vm.magnesium.value ?: 0.0),
            )
        } else {
            emptyList()
        }
        return (common + saltwater).filter { it.second > 0.0 }
    }

    private fun copyRecommendations(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.section_recommendations), text))
        Toast.makeText(requireContext(), getString(R.string.action_copied), Toast.LENGTH_SHORT).show()
    }

    private fun shareRecommendations(text: String) {
        if (text.isBlank()) return
        val intent = ShareCompat.IntentBuilder(requireActivity())
            .setType("text/plain")
            .setText(text)
            .setChooserTitle(getString(R.string.share_recommendations_title))
            .createChooserIntent()
        startActivity(intent)
    }
}
