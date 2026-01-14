package com.example.seachem_dosing.ui.calculators

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.FragmentCalculatorsBinding
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.textfield.TextInputEditText

class CalculatorsFragment : Fragment() {

    private var _binding: FragmentCalculatorsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

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

        setupVolumeDisplay()
        setupKhco3Card()
        setupEquilibriumCard()
        setupSafeCard()
        setupAptCard()
        setupNeutralCard()
        setupAcidCard()
        setupGoldCard()
        updateAllCalculations()
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
        binding.tvCurrentVolume.text = "Tank Volume: ${String.format("%.1f", litres)} L"
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

    private fun setupSafeCard() {
        val cardView = binding.cardSafe.root
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

    private fun setupExpandableCard(header: View, content: LinearLayout, expandIcon: ImageView) {
        header.setOnClickListener {
            val isExpanded = content.visibility == View.VISIBLE
            content.visibility = if (isExpanded) View.GONE else View.VISIBLE
            expandIcon.rotation = if (isExpanded) 0f else 180f
        }
    }

    private fun updateAllCalculations() {
        updateVolumeDisplay()
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

    override fun onResume() {
        super.onResume()
        updateVolumeDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
