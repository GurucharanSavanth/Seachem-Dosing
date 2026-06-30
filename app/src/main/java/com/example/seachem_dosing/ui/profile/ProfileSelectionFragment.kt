package com.example.seachem_dosing.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
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
 * behaviour is unchanged. The former `fragment_profile_selection.xml` + its
 * `bg_profile_*` drawables were removed after on-device parity verification
 * (renders + selection interaction confirmed on Pixel_10_Pro_XL, API 36).
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
        prefs.edit { putString(KEY_PROFILE, profile.id) }
    }

    companion object {
        private const val PREFS_NAME = "profile_prefs"
        private const val KEY_PROFILE = "selected_profile"
    }
}
