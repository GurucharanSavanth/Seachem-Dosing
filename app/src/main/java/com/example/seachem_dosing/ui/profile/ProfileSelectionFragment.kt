package com.example.seachem_dosing.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.theme.SeachemTheme
import com.google.android.material.transition.MaterialFadeThrough

/**
 * Hosts the Compose [ProfileSelectionScreen] (ADR-001 migration).
 *
 * Persistence + navigation stay here, identical to the former XML version, so
 * behaviour is unchanged. `fragment_profile_selection.xml` and the `bg_profile_*`
 * drawables are retained until on-device visual/a11y parity is verified, then
 * removed in a follow-up (parity gate — no emulator in this environment).
 */
class ProfileSelectionFragment : Fragment() {

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
                ProfileSelectionScreen(
                    initialSelected = loadStoredProfile(),
                    onContinue = { profile ->
                        viewModel.setProfile(profile)
                        storeProfile(profile)
                        findNavController().navigate(R.id.navigation_dashboard)
                    },
                )
            }
        }
    }

    private fun loadStoredProfile(): MainViewModel.AquariumProfile? {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_PROFILE, null) ?: return viewModel.profile.value
        return MainViewModel.AquariumProfile.fromId(value)
    }

    private fun storeProfile(profile: MainViewModel.AquariumProfile) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE, profile.id).apply()
    }

    companion object {
        private const val PREFS_NAME = "profile_prefs"
        private const val KEY_PROFILE = "selected_profile"
    }
}
