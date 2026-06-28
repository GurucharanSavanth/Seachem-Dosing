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
import com.google.android.material.transition.MaterialFadeThrough

/**
 * Hosts the Compose [DashboardScreen] (ADR-001). All volume/parameter wiring and
 * the recommendation rendering moved into the composable + [com.example.seachem_dosing.domain.engine.RecommendationEngine];
 * only clipboard copy + share (which need a Context/Activity) stay here as callbacks.
 */
class DashboardFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

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
                )
            }
        }
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
