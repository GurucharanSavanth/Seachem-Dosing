package com.example.seachem_dosing.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.FragmentProfileSelectionBinding
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialFadeThrough

class ProfileSelectionFragment : Fragment() {

    private var _binding: FragmentProfileSelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var selectedProfile: MainViewModel.AquariumProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stored = loadStoredProfile()
        if (stored != null) {
            selectedProfile = stored
            updateSelection()
            binding.btnContinue.isEnabled = true
        }

        binding.cardFreshwater.setOnClickListener {
            selectedProfile = MainViewModel.AquariumProfile.FRESHWATER
            updateSelection()
            binding.btnContinue.isEnabled = true
        }
        binding.cardSaltwater.setOnClickListener {
            selectedProfile = MainViewModel.AquariumProfile.SALTWATER
            updateSelection()
            binding.btnContinue.isEnabled = true
        }
        binding.cardPond.setOnClickListener {
            selectedProfile = MainViewModel.AquariumProfile.POND
            updateSelection()
            binding.btnContinue.isEnabled = true
        }

        binding.btnContinue.setOnClickListener {
            val profile = selectedProfile ?: return@setOnClickListener
            viewModel.setProfile(profile)
            storeProfile(profile)
            findNavController().navigate(R.id.navigation_dashboard)
        }
    }

    private fun updateSelection() {
        setCardSelected(binding.cardFreshwater, selectedProfile == MainViewModel.AquariumProfile.FRESHWATER, R.color.profile_freshwater)
        setCardSelected(binding.cardSaltwater, selectedProfile == MainViewModel.AquariumProfile.SALTWATER, R.color.profile_saltwater)
        setCardSelected(binding.cardPond, selectedProfile == MainViewModel.AquariumProfile.POND, R.color.profile_pond)
    }

    private fun setCardSelected(card: MaterialCardView, selected: Boolean, colorRes: Int) {
        val strokeWidth = if (selected) resources.getDimensionPixelSize(R.dimen.profile_card_stroke) else 0
        card.strokeWidth = strokeWidth
        card.strokeColor = ContextCompat.getColor(requireContext(), colorRes)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREFS_NAME = "profile_prefs"
        private const val KEY_PROFILE = "selected_profile"
    }
}
