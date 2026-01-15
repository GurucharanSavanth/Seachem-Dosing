package com.example.seachem_dosing.ui.calculators

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.FragmentCalculatorsBinding
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialFadeThrough

class CalculatorsFragment : Fragment() {

    private var _binding: FragmentCalculatorsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private val logTag = "CalculatorsFragment"

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
        _binding = FragmentCalculatorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyProfileUi()
        setupVolumeDisplay()
        setupKhco3Card()
        setupEquilibriumCard()
        setupSafeCard(binding.cardSafeFresh.root)
        setupAptCard()
        setupPrimeCard(binding.cardPrimeFresh.root)
        setupStabilityCard(binding.cardStabilityFresh.root)
        setupNeutralCard()
        setupAcidCard()
        setupGoldCard()
        setupWaterChangeCard(binding.cardWaterChangeFresh.root)

        setupSafeCard(binding.cardSafeSalt.root)
        setupPrimeCard(binding.cardPrimeSalt.root)
        setupStabilityCard(binding.cardStabilitySalt.root)
        setupWaterChangeCard(binding.cardWaterChangeSalt.root)

        setupSafeCard(binding.cardSafePond.root)
        setupPrimeCard(binding.cardPrimePond.root)
        setupStabilityCard(binding.cardStabilityPond.root)
        setupWaterChangeCard(binding.cardWaterChangePond.root)
        updateHardnessUnitNotes()
        syncInputsFromViewModel()
        updateAllCalculations()
        observeViewModel()
    }

    private fun setupVolumeDisplay() {
        updateVolumeDisplay()
        binding.cardVolume.setOnClickListener {
            // Navigate to dashboard to edit volume
            findNavController().navigate(R.id.navigation_dashboard)
        }
    }

    private fun updateVolumeDisplay() {
        val litres = viewModel.getEffectiveVolumeLitres()
        binding.tvCurrentVolume.text = getString(
            R.string.calc_volume_label,
            String.format("%.1f", litres)
        )
    }

    private fun setupKhco3Card() {
        val cardView = binding.cardKhco3.root
        val header = cardView.findViewById<View>(R.id.headerKhco3)
        val content = cardView.findViewById<LinearLayout>(R.id.contentKhco3)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultKhco3)
        val splitDose = cardView.findViewById<TextView>(R.id.tvSplitDose)

        val etCurrent = cardView.findViewById<TextInputEditText>(R.id.etKhCurrent)
        val etTarget = cardView.findViewById<TextInputEditText>(R.id.etKhTarget)
        val etPurity = cardView.findViewById<TextInputEditText>(R.id.etKhPurity)

        etCurrent.setText(viewModel.khCurrent.value?.toString() ?: "0")
        etTarget.setText(viewModel.khTarget.value?.toString() ?: "4")
        etPurity.setText(viewModel.khPurity.value?.toString() ?: "0.99")

        setupExpandableCard(header, content, expandIcon)

        val watcher = createTextWatcher {
            viewModel.setKhCurrent(etCurrent.text.toString().toDoubleOrNull() ?: 0.0)
            viewModel.setKhTarget(etTarget.text.toString().toDoubleOrNull() ?: 0.0)
            viewModel.setKhPurity(etPurity.text.toString().toDoubleOrNull() ?: 0.99)
            updateKhco3Result(result, splitDose)
        }
        etCurrent.addTextChangedListener(watcher)
        etTarget.addTextChangedListener(watcher)
        etPurity.addTextChangedListener(watcher)

        updateKhco3Result(result, splitDose)
    }

    private fun updateKhco3Result(result: TextView, splitDose: TextView) {
        val dosingResult = viewModel.calculateKhco3()
        if (dosingResult.dose > 0.01) {
            result.text = String.format("%.2f g", dosingResult.dose)
            if (dosingResult.splitDose.isNotEmpty()) {
                splitDose.visibility = View.VISIBLE
                splitDose.text = dosingResult.splitDose
            } else {
                splitDose.visibility = View.GONE
            }
        } else {
            result.text = getString(R.string.result_no_dose)
            splitDose.visibility = View.GONE
        }
    }

    private fun setupEquilibriumCard() {
        val cardView = binding.cardEquilibrium.root
        val header = cardView.findViewById<View>(R.id.headerEquilibrium)
        val content = cardView.findViewById<LinearLayout>(R.id.contentEquilibrium)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultEquilibrium)
        val splitDose = cardView.findViewById<TextView>(R.id.tvSplitDose)

        val etCurrent = cardView.findViewById<TextInputEditText>(R.id.etGhCurrent)
        val etTarget = cardView.findViewById<TextInputEditText>(R.id.etGhTarget)

        etCurrent.setText(viewModel.ghCurrent.value?.toString() ?: "0")
        etTarget.setText(viewModel.ghTarget.value?.toString() ?: "6")

        setupExpandableCard(header, content, expandIcon)

        val watcher = createTextWatcher {
            viewModel.setGhCurrent(etCurrent.text.toString().toDoubleOrNull() ?: 0.0)
            viewModel.setGhTarget(etTarget.text.toString().toDoubleOrNull() ?: 0.0)
            updateEquilibriumResult(result, splitDose)
        }
        etCurrent.addTextChangedListener(watcher)
        etTarget.addTextChangedListener(watcher)

        updateEquilibriumResult(result, splitDose)
    }

    private fun updateEquilibriumResult(result: TextView, splitDose: TextView) {
        val dosingResult = viewModel.calculateEquilibrium()
        if (dosingResult.dose > 0.01) {
            result.text = String.format("%.2f g", dosingResult.dose)
            if (dosingResult.splitDose.isNotEmpty()) {
                splitDose.visibility = View.VISIBLE
                splitDose.text = dosingResult.splitDose
            } else {
                splitDose.visibility = View.GONE
            }
        } else {
            result.text = getString(R.string.result_gh_above_target)
            splitDose.visibility = View.GONE
        }
    }

    private fun setupSafeCard(cardView: View) {
        val header = cardView.findViewById<View>(R.id.headerSafe)
        val content = cardView.findViewById<LinearLayout>(R.id.contentSafe)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultSafe)

        setupExpandableCard(header, content, expandIcon)
        updateSafeResult(result)
    }

    private fun updateSafeResult(result: TextView) {
        val dosingResult = viewModel.calculateSafe()
        result.text = String.format("%.2f g", dosingResult.dose)
    }

    private fun setupAptCard() {
        val cardView = binding.cardApt.root
        val header = cardView.findViewById<View>(R.id.headerApt)
        val content = cardView.findViewById<LinearLayout>(R.id.contentApt)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultApt)
        val nitrateEstimate = cardView.findViewById<TextView>(R.id.tvNitrateEstimate)

        setupExpandableCard(header, content, expandIcon)
        updateAptResult(result, nitrateEstimate)
    }

    private fun setupPrimeCard(cardView: View) {
        val header = cardView.findViewById<View>(R.id.headerPrime)
        val content = cardView.findViewById<LinearLayout>(R.id.contentPrime)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultPrime)

        setupExpandableCard(header, content, expandIcon)
        updatePrimeResult(result)
    }

    private fun updatePrimeResult(result: TextView) {
        val dose = viewModel.calculatePrimeDose()
        result.text = String.format("%.2f ml", dose)
    }

    private fun setupStabilityCard(cardView: View) {
        val header = cardView.findViewById<View>(R.id.headerStability)
        val content = cardView.findViewById<LinearLayout>(R.id.contentStability)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultStability)

        setupExpandableCard(header, content, expandIcon)
        updateStabilityResult(result)
    }

    private fun updateStabilityResult(result: TextView) {
        val dose = viewModel.calculateStabilityDose()
        result.text = String.format("%.2f ml", dose)
    }

    private fun updateAptResult(result: TextView, nitrateEstimate: TextView) {
        val aptResult = viewModel.calculateAptComplete()
        result.text = String.format("%.2f ml", aptResult.ml)
        nitrateEstimate.text = String.format(getString(R.string.nitrate_estimate), String.format("%.1f", aptResult.estimatedNitrateIncrease))
    }

    private fun setupNeutralCard() {
        val cardView = binding.cardNeutral.root
        val header = cardView.findViewById<View>(R.id.headerNeutral)
        val content = cardView.findViewById<LinearLayout>(R.id.contentNeutral)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultNeutral)
        val splitDose = cardView.findViewById<TextView>(R.id.tvSplitDose)

        val etPhCurrent = cardView.findViewById<TextInputEditText>(R.id.etPhCurrent)
        val etPhTarget = cardView.findViewById<TextInputEditText>(R.id.etPhTarget)
        val etKh = cardView.findViewById<TextInputEditText>(R.id.etNrKh)

        etPhCurrent.setText(viewModel.phCurrent.value?.toString() ?: "7.5")
        etPhTarget.setText(viewModel.phTarget.value?.toString() ?: "7.0")
        etKh.setText(viewModel.nrKh.value?.toString() ?: "4")

        setupExpandableCard(header, content, expandIcon)

        val watcher = createTextWatcher {
            viewModel.setPhCurrent(etPhCurrent.text.toString().toDoubleOrNull() ?: 7.0)
            viewModel.setPhTarget(etPhTarget.text.toString().toDoubleOrNull() ?: 7.0)
            viewModel.setNrKh(etKh.text.toString().toDoubleOrNull() ?: 4.0)
            updateNeutralResult(result, splitDose)
        }
        etPhCurrent.addTextChangedListener(watcher)
        etPhTarget.addTextChangedListener(watcher)
        etKh.addTextChangedListener(watcher)

        updateNeutralResult(result, splitDose)
    }

    private fun updateNeutralResult(result: TextView, splitDose: TextView) {
        val dosingResult = viewModel.calculateNeutralRegulator()
        if (dosingResult.dose > 0.01) {
            result.text = String.format("%.2f g", dosingResult.dose)
            if (dosingResult.splitDose.isNotEmpty()) {
                splitDose.visibility = View.VISIBLE
                splitDose.text = dosingResult.splitDose
            } else {
                splitDose.visibility = View.GONE
            }
        } else {
            result.text = getString(R.string.result_no_dose)
            splitDose.visibility = View.GONE
        }
    }

    private fun setupAcidCard() {
        val cardView = binding.cardAcid.root
        val header = cardView.findViewById<View>(R.id.headerAcid)
        val content = cardView.findViewById<LinearLayout>(R.id.contentAcid)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultAcid)
        val splitDose = cardView.findViewById<TextView>(R.id.tvSplitDose)

        val etCurrent = cardView.findViewById<TextInputEditText>(R.id.etAcidKhCurrent)
        val etTarget = cardView.findViewById<TextInputEditText>(R.id.etAcidKhTarget)

        etCurrent.setText(viewModel.acidCurrentKh.value?.toString() ?: "6")
        etTarget.setText(viewModel.acidTargetKh.value?.toString() ?: "4")

        setupExpandableCard(header, content, expandIcon)

        val watcher = createTextWatcher {
            viewModel.setAcidCurrentKh(etCurrent.text.toString().toDoubleOrNull() ?: 0.0)
            viewModel.setAcidTargetKh(etTarget.text.toString().toDoubleOrNull() ?: 0.0)
            updateAcidResult(result, splitDose)
        }
        etCurrent.addTextChangedListener(watcher)
        etTarget.addTextChangedListener(watcher)

        updateAcidResult(result, splitDose)
    }

    private fun updateAcidResult(result: TextView, splitDose: TextView) {
        val dosingResult = viewModel.calculateAcidBuffer()
        if (dosingResult.dose > 0.01) {
            result.text = String.format("%.2f g", dosingResult.dose)
            if (dosingResult.splitDose.isNotEmpty()) {
                splitDose.visibility = View.VISIBLE
                splitDose.text = dosingResult.splitDose
            } else {
                splitDose.visibility = View.GONE
            }
        } else {
            result.text = getString(R.string.result_no_dose)
            splitDose.visibility = View.GONE
        }
    }

    private fun setupGoldCard() {
        val cardView = binding.cardGold.root
        val header = cardView.findViewById<View>(R.id.headerGold)
        val content = cardView.findViewById<LinearLayout>(R.id.contentGold)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultGold)
        val doseType = cardView.findViewById<TextView>(R.id.tvDoseType)
        val splitDose = cardView.findViewById<TextView>(R.id.tvSplitDose)

        val etPhCurrent = cardView.findViewById<TextInputEditText>(R.id.etGoldPhCurrent)
        val etPhTarget = cardView.findViewById<TextInputEditText>(R.id.etGoldPhTarget)

        etPhCurrent.setText(viewModel.goldPhCurrent.value?.toString() ?: "7.0")
        etPhTarget.setText(viewModel.goldPhTarget.value?.toString() ?: "7.5")

        setupExpandableCard(header, content, expandIcon)

        val watcher = createTextWatcher {
            viewModel.setGoldPhCurrent(etPhCurrent.text.toString().toDoubleOrNull() ?: 7.0)
            viewModel.setGoldPhTarget(etPhTarget.text.toString().toDoubleOrNull() ?: 7.5)
            updateGoldResult(result, doseType, splitDose)
        }
        etPhCurrent.addTextChangedListener(watcher)
        etPhTarget.addTextChangedListener(watcher)

        updateGoldResult(result, doseType, splitDose)
    }

    private fun updateGoldResult(result: TextView, doseType: TextView, splitDose: TextView) {
        val (dosingResult, isFullDose) = viewModel.calculateGoldBuffer()
        if (dosingResult.dose > 0.01) {
            result.text = String.format("%.2f g", dosingResult.dose)
            doseType.text = if (isFullDose) getString(R.string.result_full_dose) else getString(R.string.result_half_dose)
            if (dosingResult.splitDose.isNotEmpty()) {
                splitDose.visibility = View.VISIBLE
                splitDose.text = dosingResult.splitDose
            } else {
                splitDose.visibility = View.GONE
            }
        } else {
            result.text = getString(R.string.result_no_dose)
            doseType.text = ""
            splitDose.visibility = View.GONE
        }
    }

    private fun setupWaterChangeCard(cardView: View) {
        val header = cardView.findViewById<View>(R.id.headerWaterChange)
        val content = cardView.findViewById<LinearLayout>(R.id.contentWaterChange)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultWaterChange)
        val percentInput = cardView.findViewById<TextInputEditText>(R.id.etWaterChangePercent)

        setupExpandableCard(header, content, expandIcon)
        val watcher = createTextWatcher {
            updateWaterChangeResult(result, percentInput)
        }
        percentInput.addTextChangedListener(watcher)
        updateWaterChangeResult(result, percentInput)
    }

    private fun updateWaterChangeResult(result: TextView, percentInput: TextInputEditText) {
        val percent = percentInput.text?.toString()?.toDoubleOrNull() ?: 0.0
        val litres = viewModel.calculateWaterChangeLitres(percent)
        val unit = viewModel.volumeUnit.value ?: "L"
        val unitLabel = when (unit) {
            "US" -> getString(R.string.unit_us_gal_short)
            "UK" -> getString(R.string.unit_uk_gal_short)
            else -> getString(R.string.unit_l_short)
        }
        val converted = Calculations.fromLitres(litres, unit)
        result.text = getString(
            R.string.result_change_volume_unit,
            String.format("%.1f", litres),
            String.format("%.1f", converted),
            unitLabel
        )
    }

    private fun setupExpandableCard(header: View, content: LinearLayout, expandIcon: ImageView) {
        header.setOnClickListener {
            val isExpanded = content.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(header.parent as ViewGroup, AutoTransition())
            content.visibility = if (isExpanded) View.GONE else View.VISIBLE
            expandIcon.animate().rotation(if (isExpanded) 0f else 180f).setDuration(200).start()
        }
    }

    private fun updateAllCalculations() {
        updateVolumeDisplay()
        val khCard = binding.cardKhco3.root
        updateKhco3Result(
            khCard.findViewById(R.id.tvResultKhco3),
            khCard.findViewById(R.id.tvSplitDose)
        )

        val eqCard = binding.cardEquilibrium.root
        updateEquilibriumResult(
            eqCard.findViewById(R.id.tvResultEquilibrium),
            eqCard.findViewById(R.id.tvSplitDose)
        )

        updateSafeResult(binding.cardSafeFresh.root.findViewById(R.id.tvResultSafe))
        updateSafeResult(binding.cardSafeSalt.root.findViewById(R.id.tvResultSafe))
        updateSafeResult(binding.cardSafePond.root.findViewById(R.id.tvResultSafe))

        val aptCard = binding.cardApt.root
        updateAptResult(
            aptCard.findViewById(R.id.tvResultApt),
            aptCard.findViewById(R.id.tvNitrateEstimate)
        )

        updatePrimeResult(binding.cardPrimeFresh.root.findViewById(R.id.tvResultPrime))
        updatePrimeResult(binding.cardPrimeSalt.root.findViewById(R.id.tvResultPrime))
        updatePrimeResult(binding.cardPrimePond.root.findViewById(R.id.tvResultPrime))
        updateStabilityResult(binding.cardStabilityFresh.root.findViewById(R.id.tvResultStability))
        updateStabilityResult(binding.cardStabilitySalt.root.findViewById(R.id.tvResultStability))
        updateStabilityResult(binding.cardStabilityPond.root.findViewById(R.id.tvResultStability))

        val neutralCard = binding.cardNeutral.root
        updateNeutralResult(
            neutralCard.findViewById(R.id.tvResultNeutral),
            neutralCard.findViewById(R.id.tvSplitDose)
        )

        val acidCard = binding.cardAcid.root
        updateAcidResult(
            acidCard.findViewById(R.id.tvResultAcid),
            acidCard.findViewById(R.id.tvSplitDose)
        )

        val goldCard = binding.cardGold.root
        updateGoldResult(
            goldCard.findViewById(R.id.tvResultGold),
            goldCard.findViewById(R.id.tvDoseType),
            goldCard.findViewById(R.id.tvSplitDose)
        )

        updateWaterChangeResult(
            binding.cardWaterChangeFresh.root.findViewById(R.id.tvResultWaterChange),
            binding.cardWaterChangeFresh.root.findViewById(R.id.etWaterChangePercent)
        )
        updateWaterChangeResult(
            binding.cardWaterChangePond.root.findViewById(R.id.tvResultWaterChange),
            binding.cardWaterChangePond.root.findViewById(R.id.etWaterChangePercent)
        )
        updateWaterChangeResult(
            binding.cardWaterChangeSalt.root.findViewById(R.id.tvResultWaterChange),
            binding.cardWaterChangeSalt.root.findViewById(R.id.etWaterChangePercent)
        )
    }

    private fun updateHardnessUnitNotes() {
        val ghUnit = viewModel.ghUnit.value ?: "dh"
        val khUnit = viewModel.khUnit.value ?: "dh"
        val ghDisplay = getString(R.string.unit_dgh)
        val khDisplay = getString(R.string.unit_dkh)

        val ghTemplate = if (ghUnit == "ppm") R.string.label_unit_note_converted else R.string.label_unit_note
        val khTemplate = if (khUnit == "ppm") R.string.label_unit_note_converted else R.string.label_unit_note

        binding.cardEquilibrium.root.findViewById<TextView>(R.id.tvGhUnitNote)?.text =
            getString(ghTemplate, ghDisplay)

        binding.cardKhco3.root.findViewById<TextView>(R.id.tvKhUnitNote)?.text =
            getString(khTemplate, khDisplay)

        binding.cardNeutral.root.findViewById<TextView>(R.id.tvKhUnitNote)?.text =
            getString(khTemplate, khDisplay)

        binding.cardAcid.root.findViewById<TextView>(R.id.tvKhUnitNote)?.text =
            getString(khTemplate, khDisplay)
    }

    private fun applyProfileUi() {
        val profile = viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER
        binding.groupCalcFreshwater.visibility =
            if (profile == MainViewModel.AquariumProfile.FRESHWATER) View.VISIBLE else View.GONE
        binding.groupCalcSaltwater.visibility =
            if (profile == MainViewModel.AquariumProfile.SALTWATER) View.VISIBLE else View.GONE
        binding.groupCalcPond.visibility =
            if (profile == MainViewModel.AquariumProfile.POND) View.VISIBLE else View.GONE

        val badgeText = when (profile) {
            MainViewModel.AquariumProfile.FRESHWATER -> R.string.profile_badge_freshwater
            MainViewModel.AquariumProfile.SALTWATER -> R.string.profile_badge_saltwater
            MainViewModel.AquariumProfile.POND -> R.string.profile_badge_pond
        }
        val badgeColor = when (profile) {
            MainViewModel.AquariumProfile.FRESHWATER -> R.color.profile_freshwater
            MainViewModel.AquariumProfile.SALTWATER -> R.color.profile_saltwater
            MainViewModel.AquariumProfile.POND -> R.color.profile_pond
        }
        binding.tvProfileBadge.text = getString(badgeText)
        binding.tvProfileBadge.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), badgeColor))
        binding.tvProfileBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
    }

    private fun syncInputsFromViewModel() {
        val khCard = binding.cardKhco3.root
        setIfNotFocused(khCard.findViewById(R.id.etKhCurrent), viewModel.khCurrent.value ?: 0.0, 2)
        setIfNotFocused(khCard.findViewById(R.id.etKhTarget), viewModel.khTarget.value ?: 4.0, 2)
        setIfNotFocused(khCard.findViewById(R.id.etKhPurity), viewModel.khPurity.value ?: 0.99, 2)

        val eqCard = binding.cardEquilibrium.root
        setIfNotFocused(eqCard.findViewById(R.id.etGhCurrent), viewModel.ghCurrent.value ?: 0.0, 2)
        setIfNotFocused(eqCard.findViewById(R.id.etGhTarget), viewModel.ghTarget.value ?: 6.0, 2)

        val neutralCard = binding.cardNeutral.root
        setIfNotFocused(neutralCard.findViewById(R.id.etPhCurrent), viewModel.phCurrent.value ?: 7.5, 2)
        setIfNotFocused(neutralCard.findViewById(R.id.etPhTarget), viewModel.phTarget.value ?: 7.0, 2)
        setIfNotFocused(neutralCard.findViewById(R.id.etNrKh), viewModel.nrKh.value ?: 4.0, 2)

        val acidCard = binding.cardAcid.root
        setIfNotFocused(acidCard.findViewById(R.id.etAcidKhCurrent), viewModel.acidCurrentKh.value ?: 6.0, 2)
        setIfNotFocused(acidCard.findViewById(R.id.etAcidKhTarget), viewModel.acidTargetKh.value ?: 4.0, 2)

        val goldCard = binding.cardGold.root
        setIfNotFocused(goldCard.findViewById(R.id.etGoldPhCurrent), viewModel.goldPhCurrent.value ?: 7.0, 2)
        setIfNotFocused(goldCard.findViewById(R.id.etGoldPhTarget), viewModel.goldPhTarget.value ?: 7.5, 2)

        Log.d(logTag, "Synced calculator inputs from dashboard parameters.")
    }

    private fun setIfNotFocused(editText: TextInputEditText?, value: Double, decimals: Int) {
        if (editText == null || editText.isFocused) return
        val formatted = "%.${decimals}f".format(value)
        if (editText.text?.toString() != formatted) {
            editText.setText(formatted)
        }
    }

    private fun createTextWatcher(onTextChanged: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.ghUnit.observe(viewLifecycleOwner) {
            updateHardnessUnitNotes()
            syncInputsFromViewModel()
            updateAllCalculations()
        }
        viewModel.khUnit.observe(viewLifecycleOwner) {
            updateHardnessUnitNotes()
            syncInputsFromViewModel()
            updateAllCalculations()
        }
        viewModel.volumeUnit.observe(viewLifecycleOwner) {
            updateAllCalculations()
        }
        viewModel.profile.observe(viewLifecycleOwner) {
            applyProfileUi()
            updateAllCalculations()
        }
    }

    override fun onResume() {
        super.onResume()
        applyProfileUi()
        syncInputsFromViewModel()
        updateHardnessUnitNotes()
        updateVolumeDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
