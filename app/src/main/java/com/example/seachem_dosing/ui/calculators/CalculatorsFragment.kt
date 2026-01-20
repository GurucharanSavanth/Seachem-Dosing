package com.example.seachem_dosing.ui.calculators

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.DialogEditVolumeBinding
import com.example.seachem_dosing.databinding.FragmentCalculatorsBinding
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.logic.SeachemCalculations
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.util.DebouncedTextWatcher
import com.example.seachem_dosing.util.TextWatcherManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialFadeThrough

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class CalculatorsFragment : Fragment() {

    private var _binding: FragmentCalculatorsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    // TextWatcher manager for proper cleanup to prevent memory leaks
    private val textWatcherManager = TextWatcherManager()

    private companion object {
        const val INPUT_DEBOUNCE_MS = 250L
    }

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

        // Offload heavy initialization to a coroutine with better frame pacing
        // Use Default dispatcher for setup work to avoid blocking main thread
        viewLifecycleOwner.lifecycleScope.launch {
            // Setup cards in smaller batches with yields between to prevent frame drops
            // Batch 1: Planted basics
            setupCardBatch {
                setupUniversalCard(binding.cardFlourish.root, "flourish", SeachemCalculations.Product.FLOURISH, R.string.calc_flourish_title, R.string.calc_flourish_subtitle, showInputs = false)
                setupUniversalCard(binding.cardFlourishTrace.root, "flourish_trace", SeachemCalculations.Product.FLOURISH_TRACE, R.string.calc_flourish_trace_title, R.string.calc_flourish_trace_subtitle, showInputs = false)
            }

            // Batch 2: Planted nutrients
            setupCardBatch {
                setupUniversalCard(binding.cardFlourishIron.root, "flourish_iron", SeachemCalculations.Product.FLOURISH_IRON, R.string.calc_flourish_iron_title, R.string.calc_flourish_iron_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_fe, targetLabelRes = R.string.label_target_fe)
                setupUniversalCard(binding.cardFlourishNitrogen.root, "flourish_nitrogen", SeachemCalculations.Product.FLOURISH_NITROGEN, R.string.calc_flourish_nitrogen_title, R.string.calc_flourish_nitrogen_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_n, targetLabelRes = R.string.label_target_n)
            }

            setupCardBatch {
                setupUniversalCard(binding.cardFlourishPhosphorus.root, "flourish_phosphorus", SeachemCalculations.Product.FLOURISH_PHOSPHORUS, R.string.calc_flourish_phosphorus_title, R.string.calc_flourish_phosphorus_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_p, targetLabelRes = R.string.label_target_p)
                setupUniversalCard(binding.cardFlourishPotassium.root, "flourish_potassium", SeachemCalculations.Product.FLOURISH_POTASSIUM, R.string.calc_flourish_potassium_title, R.string.calc_flourish_potassium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_k, targetLabelRes = R.string.label_target_k)
            }

            // Batch 3: Buffers
            setupCardBatch {
                setupUniversalCard(binding.cardAlkalineBuffer.root, "alkaline_buffer", SeachemCalculations.Product.ALKALINE_BUFFER, R.string.calc_alkaline_buffer_title, R.string.calc_alkaline_buffer_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_kh_meq, targetLabelRes = R.string.label_target_kh_meq)
                setupUniversalCard(binding.cardAcidBuffer.root, "acid_buffer", SeachemCalculations.Product.ACID_BUFFER, R.string.calc_acid_title, R.string.calc_acid_subtitle, SeachemCalculations.UnitScale.DKH, true,
                    currentLabelRes = R.string.label_current_kh, targetLabelRes = R.string.label_target_kh)
            }

            setupCardBatch {
                setupUniversalCard(binding.cardPotassiumBicarbonate.root, "khco3", SeachemCalculations.Product.POTASSIUM_BICARBONATE, R.string.calc_khco3_title, R.string.calc_khco3_subtitle, SeachemCalculations.UnitScale.DKH, true,
                    currentLabelRes = R.string.label_current_kh, targetLabelRes = R.string.label_target_kh)
                setupUniversalCard(binding.cardEquilibrium.root, "equilibrium", SeachemCalculations.Product.EQUILIBRIUM, R.string.calc_equilibrium_title, R.string.calc_equilibrium_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_gh_meq, targetLabelRes = R.string.label_target_gh_meq)
                setupUniversalCard(binding.cardNeutralRegulator.root, "neutral_regulator", SeachemCalculations.Product.NEUTRAL_REGULATOR, R.string.calc_neutral_title, R.string.calc_neutral_subtitle,
                    showScale = false, currentLabelRes = R.string.label_current_ph, targetLabelRes = R.string.label_target_ph)
            }

            // Batch 4: Saltwater basics
            setupCardBatch {
                setupUniversalCard(binding.cardReefAdvCalcium.root, "reef_adv_calcium", SeachemCalculations.Product.REEF_ADVANTAGE_CALCIUM, R.string.calc_reef_adv_calcium_title, R.string.calc_reef_adv_calcium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_ca, targetLabelRes = R.string.label_target_ca)
                setupUniversalCard(binding.cardReefAdvMagnesium.root, "reef_adv_magnesium", SeachemCalculations.Product.REEF_ADVANTAGE_MAGNESIUM, R.string.calc_reef_adv_magnesium_title, R.string.calc_reef_adv_magnesium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_mg, targetLabelRes = R.string.label_target_mg)
                setupUniversalCard(binding.cardReefAdvStrontium.root, "reef_adv_strontium", SeachemCalculations.Product.REEF_ADVANTAGE_STRONTIUM, R.string.calc_reef_adv_strontium_title, R.string.calc_reef_adv_strontium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_sr, targetLabelRes = R.string.label_target_sr)
            }

            // Batch 5: Reef buffers
            setupCardBatch {
                setupUniversalCard(binding.cardReefBuffer.root, "reef_buffer", SeachemCalculations.Product.REEF_BUFFER, R.string.calc_reef_buffer_title, R.string.calc_reef_buffer_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_alk, targetLabelRes = R.string.label_target_alk)
                setupUniversalCard(binding.cardReefBuilder.root, "reef_builder", SeachemCalculations.Product.REEF_BUILDER, R.string.calc_reef_builder_title, R.string.calc_reef_builder_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_alk, targetLabelRes = R.string.label_target_alk)
                setupUniversalCard(binding.cardReefCalcium.root, "reef_calcium", SeachemCalculations.Product.REEF_CALCIUM, R.string.calc_reef_calcium_title, R.string.calc_reef_calcium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_ca, targetLabelRes = R.string.label_target_ca)
            }

            // Batch 6: More reef
            setupCardBatch {
                setupUniversalCard(binding.cardReefCarbonate.root, "reef_carbonate", SeachemCalculations.Product.REEF_CARBONATE, R.string.calc_reef_carbonate_title, R.string.calc_reef_carbonate_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_alk, targetLabelRes = R.string.label_target_alk)
                setupUniversalCard(binding.cardReefComplete.root, "reef_complete", SeachemCalculations.Product.REEF_COMPLETE, R.string.calc_reef_complete_title, R.string.calc_reef_complete_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_ca, targetLabelRes = R.string.label_target_ca)
            }

            // Batch 7: Reef fusion
            setupCardBatch {
                setupUniversalCard(binding.cardReefFusion1.root, "reef_fusion1", SeachemCalculations.Product.REEF_FUSION_1, R.string.calc_reef_fusion1_title, R.string.calc_reef_fusion1_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_ca, targetLabelRes = R.string.label_target_ca)
                setupUniversalCard(binding.cardReefFusion2.root, "reef_fusion2", SeachemCalculations.Product.REEF_FUSION_2, R.string.calc_reef_fusion2_title, R.string.calc_reef_fusion2_subtitle, SeachemCalculations.UnitScale.MEQ_L, true,
                    currentLabelRes = R.string.label_current_alk, targetLabelRes = R.string.label_target_alk)
            }

            // Batch 8: Reef trace elements
            setupCardBatch {
                setupUniversalCard(binding.cardReefIodide.root, "reef_iodide", SeachemCalculations.Product.REEF_IODIDE, R.string.calc_reef_iodide_title, R.string.calc_reef_iodide_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_i, targetLabelRes = R.string.label_target_i)
                setupUniversalCard(binding.cardReefStrontium.root, "reef_strontium", SeachemCalculations.Product.REEF_STRONTIUM, R.string.calc_reef_strontium_title, R.string.calc_reef_strontium_subtitle,
                    showScale = true, currentLabelRes = R.string.label_current_sr, targetLabelRes = R.string.label_target_sr)
            }

            // Batch 9: Substrate and Salt Mix
            setupCardBatch {
                setupSubstrateCard()
                setupSaltMixCard()
            }

            // Batch 10: Quick doses
            setupCardBatch {
                setupPrimeCard(binding.cardPrimeFresh.root)
                setupPrimeCard(binding.cardPrimeSalt.root)
                setupStabilityCard(binding.cardStabilityFresh.root)
                setupStabilityCard(binding.cardStabilitySalt.root)
            }

            setupCardBatch {
                setupSafeCard(binding.cardSafeFresh.root)
                setupSafeCard(binding.cardSafeSalt.root)
                setupWaterChangeCard(binding.cardWaterChangeFresh.root)
                setupWaterChangeCard(binding.cardWaterChangeSalt.root)
            }
        }

        observeViewModel()
    }

    /**
     * Helper function to setup cards in a batch with proper frame pacing.
     * Yields after each batch to allow the UI to render frames.
     */
    private suspend fun setupCardBatch(block: () -> Unit) {
        // Check if view is still valid
        if (_binding == null) return
        block()
        yield() // Allow UI to render frames
    }

    private fun setupUniversalCard(
        cardView: View,
        calculatorId: String,
        product: SeachemCalculations.Product,
        titleRes: Int,
        subtitleRes: Int,
        defaultScale: SeachemCalculations.UnitScale = SeachemCalculations.UnitScale.PPM,
        showScale: Boolean = false,
        showInputs: Boolean = true,
        currentLabelRes: Int = R.string.label_current,
        targetLabelRes: Int = R.string.label_target
    ) {
        val header = cardView.findViewById<View>(R.id.headerUniversal)
        val content = cardView.findViewById<LinearLayout>(R.id.contentUniversal)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val tvTitle = cardView.findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = cardView.findViewById<TextView>(R.id.tvSubtitle)
        val tvResult = cardView.findViewById<TextView>(R.id.tvResult)
        val tvSecondResult = cardView.findViewById<TextView>(R.id.tvSecondResult)
        
        val etCurrent = cardView.findViewById<TextInputEditText>(R.id.etCurrent)
        val etTarget = cardView.findViewById<TextInputEditText>(R.id.etTarget)
        val layoutCurrent = cardView.findViewById<TextInputLayout>(R.id.layoutCurrent)
        val layoutTarget = cardView.findViewById<TextInputLayout>(R.id.layoutTarget)
        
        val layoutScale = cardView.findViewById<TextInputLayout>(R.id.layoutUnitScale)
        val spinnerScale = cardView.findViewById<AutoCompleteTextView>(R.id.spinnerUnitScale)

        tvTitle.setText(titleRes)
        tvSubtitle.setText(subtitleRes)
        
        setupExpandableCard(header, content, expandIcon)

        // Visibility Logic
        layoutCurrent.visibility = if (showInputs) View.VISIBLE else View.GONE
        layoutTarget.visibility = if (showInputs) View.VISIBLE else View.GONE

        // Unit Scale Logic
        val isGh = product == SeachemCalculations.Product.EQUILIBRIUM
        val degreeLabel = if (isGh) "dGH" else "dKH"
        
        val scales = if (showScale) arrayOf("meq/L", degreeLabel, "ppm") else emptyArray()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, scales)
        spinnerScale.setAdapter(adapter)
        
        if (showScale) {
            layoutScale.visibility = View.VISIBLE
            val defIndex = when(defaultScale) {
                SeachemCalculations.UnitScale.MEQ_L -> 0
                SeachemCalculations.UnitScale.DKH -> 1
                SeachemCalculations.UnitScale.PPM -> 2
                else -> 2
            }
            spinnerScale.setText(scales.getOrElse(defIndex) { scales[0] }, false)
        } else {
            layoutScale.visibility = View.GONE
        }

        // Logic
        fun calculate() {
            val current = etCurrent.text?.toString()?.toDoubleOrNull() ?: 0.0
            val target = etTarget.text?.toString()?.toDoubleOrNull() ?: 0.0
            val scaleStr = spinnerScale.text.toString()
            
            // Dynamic Hints
            if (showScale && showInputs) {
                val unitSuffix = when {
                    scaleStr.contains("meq") -> "(meq/L)"
                    scaleStr.contains("dKH") -> "(°dKH)"
                    scaleStr.contains("dGH") -> "(°dGH)"
                    else -> "(ppm)"
                }
                layoutCurrent.hint = "${getString(currentLabelRes)} $unitSuffix"
                layoutTarget.hint = "${getString(targetLabelRes)} $unitSuffix"
            } else if (showInputs) {
                layoutCurrent.hint = getString(currentLabelRes)
                layoutTarget.hint = getString(targetLabelRes)
            }

            val scale = when {
                scaleStr.contains("meq") -> SeachemCalculations.UnitScale.MEQ_L
                scaleStr.contains("dKH") || scaleStr.contains("dGH") -> SeachemCalculations.UnitScale.DKH
                else -> SeachemCalculations.UnitScale.PPM
            }
            
            // Save inputs to ViewModel (optional, for persistence)
            viewModel.setInput("${calculatorId}_current", current)
            viewModel.setInput("${calculatorId}_target", target)

            val result = viewModel.calculateUniversal(product, current, target, scale)
            
            if (result.primaryValue.toDouble() > 0.0) {
                tvResult.text = "${result.primaryValue.toPlainString()} ${result.primaryUnit}"
                tvSecondResult.visibility = View.VISIBLE
                tvSecondResult.text = "Alternate: ${result.secondaryValue.toPlainString()} ${result.secondaryUnit}"
            } else {
                tvResult.text = getString(R.string.result_no_dose)
                tvSecondResult.visibility = View.GONE
            }
        }

        // Use debounced watchers to prevent frame drops from rapid calculations
        val currentWatcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        val targetWatcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }

        etCurrent.addTextChangedListener(currentWatcher)
        etTarget.addTextChangedListener(targetWatcher)
        spinnerScale.setOnItemClickListener { _, _, _, _ -> calculate() }
        
        // Restore Inputs from ViewModel
        val savedCurrent = viewModel.getInput("${calculatorId}_current").value
        val savedTarget = viewModel.getInput("${calculatorId}_target").value
        if (savedCurrent != null && savedCurrent > 0) etCurrent.setText(savedCurrent.toString())
        if (savedTarget != null && savedTarget > 0) etTarget.setText(savedTarget.toString())
        
        calculate() // Trigger initial calc and hint update
    }

    private fun setupSubstrateCard() {
        val card = binding.cardSubstrate.root
        val header = card.findViewById<View>(R.id.headerSubstrate)
        val content = card.findViewById<LinearLayout>(R.id.contentSubstrate)
        val expandIcon = card.findViewById<ImageView>(R.id.iconExpand)
        val result = card.findViewById<TextView>(R.id.tvResultSubstrate)
        val spinner = card.findViewById<AutoCompleteTextView>(R.id.spinnerSubstrateProduct)
        
        val etL = card.findViewById<TextInputEditText>(R.id.etLength)
        val etW = card.findViewById<TextInputEditText>(R.id.etWidth)
        val etD = card.findViewById<TextInputEditText>(R.id.etDepth)
        val toggleUnit = card.findViewById<MaterialButtonToggleGroup>(R.id.toggleSubstrateUnit)

        // Expand by default logic
        content.visibility = View.VISIBLE
        expandIcon.rotation = 180f
        
        setupExpandableCard(header, content, expandIcon)

        val products = listOf("Flourite", "Flourite Black", "Flourite Black Sand", "Flourite Dark", "Flourite Red", "Flourite Sand", "Gray Coast", "Meridian", "Onyx", "Onyx Sand", "Pearl Beach")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, products)
        spinner.setAdapter(adapter)
        spinner.setText(products[0], false)
        
        fun calculate() {
            val l = etL.text?.toString()?.toDoubleOrNull() ?: 0.0
            val w = etW.text?.toString()?.toDoubleOrNull() ?: 0.0
            val d = etD.text?.toString()?.toDoubleOrNull() ?: 0.0
            
            val unit = if (toggleUnit.checkedButtonId == R.id.btnUnitCm) "cm" else "in"
            val prodIndex = products.indexOf(spinner.text.toString()).coerceAtLeast(0)
            
            viewModel.setSubLength(l)
            viewModel.setSubWidth(w)
            viewModel.setSubDepth(d)
            viewModel.setSubUnit(unit)
            viewModel.setSubProduct(prodIndex)
            
            val res = viewModel.calculateSubstrate(prodIndex)
            val bags = res.primaryValue.toDouble()
            // Estimate weight: standard 7kg per bag (approx)
            val weight = bags * 7.0
            result.text = "${res.primaryValue.toPlainString()} Bags (~${String.format("%.1f", weight)} kg)"
        }

        // Use debounced watchers
        val watcherL = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        val watcherW = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        val watcherD = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        etL.addTextChangedListener(watcherL)
        etW.addTextChangedListener(watcherW)
        etD.addTextChangedListener(watcherD)
        spinner.setOnItemClickListener { _, _, _, _ -> calculate() }
        toggleUnit.addOnButtonCheckedListener { _, _, _ -> calculate() }
    }

    private fun setupVolumeDisplay() {
        updateVolumeDisplay()
        binding.cardVolume.setOnClickListener {
            showVolumeEditDialog()
        }
    }

    private fun updateVolumeDisplay() {
        val litres = viewModel.getEffectiveVolumeLitres()
        binding.tvCurrentVolume.text = getString(
            R.string.calc_volume_label,
            String.format("%.1f", litres)
        )
    }

    // Reuse existing Prime/Safe/WaterChange logic or simplified
    private fun setupPrimeCard(cardView: View) {
        val result = cardView.findViewById<TextView>(R.id.tvResultPrime)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        setupExpandableCard(cardView.findViewById(R.id.headerPrime), cardView.findViewById(R.id.contentPrime), expandIcon)
        result.text = String.format("%.1f ml", viewModel.calculatePrimeDose())
    }
    
    private fun setupStabilityCard(cardView: View) {
        val result = cardView.findViewById<TextView>(R.id.tvResultStability)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        setupExpandableCard(cardView.findViewById(R.id.headerStability), cardView.findViewById(R.id.contentStability), expandIcon)
        result.text = String.format("%.1f ml", viewModel.calculateStabilityDose())
    }
    
    private fun setupSafeCard(cardView: View) {
        val result = cardView.findViewById<TextView>(R.id.tvResultSafe)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        setupExpandableCard(cardView.findViewById(R.id.headerSafe), cardView.findViewById(R.id.contentSafe), expandIcon)
        result.text = String.format("%.2f g", viewModel.calculateSafeSimple())
    }
    
    private fun setupWaterChangeCard(cardView: View) {
        val header = cardView.findViewById<View>(R.id.headerWaterChange)
        val content = cardView.findViewById<LinearLayout>(R.id.contentWaterChange)
        val expandIcon = cardView.findViewById<ImageView>(R.id.iconExpand)
        val result = cardView.findViewById<TextView>(R.id.tvResultWaterChange)
        val percentInput = cardView.findViewById<TextInputEditText>(R.id.etWaterChangePercent)
        
        // Expand by default
        content.visibility = View.VISIBLE
        expandIcon.rotation = 180f
        
        setupExpandableCard(header, content, expandIcon)
        
        val watcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { p ->
            result.text = String.format("%.1f L", viewModel.calculateWaterChangeLitres(p))
        }
        percentInput.addTextChangedListener(watcher)
        // Initial calc
        result.text = String.format("%.1f L", viewModel.calculateWaterChangeLitres(percentInput.text.toString().toDoubleOrNull() ?: 0.0))
    }

    private fun setupExpandableCard(header: View, content: LinearLayout, expandIcon: ImageView) {
        header.setOnClickListener {
            val isExpanded = content.visibility == View.VISIBLE
            TransitionManager.beginDelayedTransition(header.parent as ViewGroup, AutoTransition())
            content.visibility = if (isExpanded) View.GONE else View.VISIBLE
            expandIcon.animate().rotation(if (isExpanded) 0f else 180f).setDuration(200).start()
        }
    }
    
    private fun showVolumeEditDialog() {
        val dialogBinding = DialogEditVolumeBinding.inflate(layoutInflater)

        val volumeUnits = arrayOf(getString(R.string.unit_us_gallon), getString(R.string.unit_litre), getString(R.string.unit_uk_gallon))
        val volumeAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, volumeUnits)
        dialogBinding.dialogSpinnerVolumeUnit.setAdapter(volumeAdapter)
        val currentVolumeUnit = viewModel.volumeUnit.value ?: "US"
        val volumeIndex = when (currentVolumeUnit) {
            "L" -> 1
            "UK" -> 2
            else -> 0
        }
        dialogBinding.dialogSpinnerVolumeUnit.setText(volumeUnits[volumeIndex], false)
        var selectedVolumeUnit = currentVolumeUnit
        dialogBinding.dialogSpinnerVolumeUnit.setOnItemClickListener { _, _, position, _ ->
            selectedVolumeUnit = when (position) {
                0 -> "US"
                1 -> "L"
                2 -> "UK"
                else -> "US"
            }
        }

        val dimUnits = arrayOf(getString(R.string.unit_cm), getString(R.string.unit_in), getString(R.string.unit_ft))
        val dimAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, dimUnits)
        dialogBinding.dialogSpinnerDimUnit.setAdapter(dimAdapter)
        val currentDimUnit = viewModel.dimUnit.value ?: "cm"
        val dimIndex = when (currentDimUnit) {
            "in" -> 1
            "ft" -> 2
            else -> 0
        }
        dialogBinding.dialogSpinnerDimUnit.setText(dimUnits[dimIndex], false)
        var selectedDimUnit = currentDimUnit

        dialogBinding.dialogEtVolume.setText(viewModel.volume.value?.toString() ?: "10")
        dialogBinding.dialogEtLength.setText(viewModel.dimLength.value?.toString() ?: "60")
        dialogBinding.dialogEtBreadth.setText(viewModel.dimBreadth.value?.toString() ?: "30")
        dialogBinding.dialogEtHeight.setText(viewModel.dimHeight.value?.toString() ?: "40")

        fun updateCalculatedVolumeText() {
            val length = dialogBinding.dialogEtLength.text?.toString()?.toDoubleOrNull() ?: 0.0
            val breadth = dialogBinding.dialogEtBreadth.text?.toString()?.toDoubleOrNull() ?: 0.0
            val height = dialogBinding.dialogEtHeight.text?.toString()?.toDoubleOrNull() ?: 0.0
            val litres = Calculations.dimensionsToLitres(length, breadth, height, selectedDimUnit)
            dialogBinding.dialogCalculatedVolume.text =
                getString(R.string.calculated_volume, String.format("%.1f", litres))
        }

        dialogBinding.dialogSpinnerDimUnit.setOnItemClickListener { _, _, position, _ ->
            selectedDimUnit = when(position) { 0->"cm"; 1->"in"; 2->"ft"; else->"cm" }
            updateCalculatedVolumeText()
        }

        val currentMode = viewModel.volumeMode.value ?: "direct"
        val isLbhMode = currentMode == "lbh"
        dialogBinding.dialogToggleVolumeMode.check(if (isLbhMode) R.id.dialogBtnLbh else R.id.dialogBtnDirect)
        dialogBinding.dialogLayoutDirect.visibility = if (isLbhMode) View.GONE else View.VISIBLE
        dialogBinding.dialogLayoutDimensions.visibility = if (isLbhMode) View.VISIBLE else View.GONE
        updateCalculatedVolumeText()

        dialogBinding.dialogToggleVolumeMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val useLbh = checkedId == R.id.dialogBtnLbh
                dialogBinding.dialogLayoutDirect.visibility = if (useLbh) View.GONE else View.VISIBLE
                dialogBinding.dialogLayoutDimensions.visibility = if (useLbh) View.VISIBLE else View.GONE
                if (useLbh) {
                    updateCalculatedVolumeText()
                }
            }
        }

        val dimWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateCalculatedVolumeText()
            }
        }
        dialogBinding.dialogEtLength.addTextChangedListener(dimWatcher)
        dialogBinding.dialogEtBreadth.addTextChangedListener(dimWatcher)
        dialogBinding.dialogEtHeight.addTextChangedListener(dimWatcher)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.volume_edit_title))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.action_ok)) { _, _ ->
                val useLbh = dialogBinding.dialogToggleVolumeMode.checkedButtonId == R.id.dialogBtnLbh
                if (useLbh) {
                    viewModel.setVolumeMode("lbh")
                    viewModel.setDimLength(dialogBinding.dialogEtLength.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    viewModel.setDimBreadth(dialogBinding.dialogEtBreadth.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    viewModel.setDimHeight(dialogBinding.dialogEtHeight.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    viewModel.setDimUnit(selectedDimUnit)
                } else {
                    viewModel.setVolumeMode("direct")
                    viewModel.setVolume(dialogBinding.dialogEtVolume.text?.toString()?.toDoubleOrNull() ?: 0.0)
                    viewModel.setVolumeUnit(selectedVolumeUnit)
                }
                updateVolumeDisplay()
                // Recalculate all (handled by observers in real app, but here we might need to trigger manually if needed, 
                // but inputs are separated. Changing volume triggers observers in Fragment if we observe volume. 
                // We observe volume in 'observeViewModel')
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun setupSaltMixCard() {
        val card = binding.cardSaltMix.root
        val header = card.findViewById<View>(R.id.headerSaltMix)
        val content = card.findViewById<LinearLayout>(R.id.contentSaltMix)
        val expandIcon = card.findViewById<ImageView>(R.id.iconExpand)
        val resultPrimary = card.findViewById<TextView>(R.id.tvResultSaltMix)
        val resultDetails = card.findViewById<TextView>(R.id.tvResultSaltDetails)
        
        val spinner = card.findViewById<AutoCompleteTextView>(R.id.spinnerSaltProduct)
        val etVol = card.findViewById<TextInputEditText>(R.id.etSaltVolume)
        val etCur = card.findViewById<TextInputEditText>(R.id.etSaltCurrent)
        val etTar = card.findViewById<TextInputEditText>(R.id.etSaltTarget)

        // Default state
        setupExpandableCard(header, content, expandIcon)
        content.visibility = View.VISIBLE
        expandIcon.rotation = 180f
        resultPrimary.text = "---"

        // Setup Spinner
        val products = com.example.seachem_dosing.logic.SaltMixCalculations.SALT_MIX_PRODUCTS.keys.toList()
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_menu, products)
        spinner.setAdapter(adapter)
        
        // Restore/Init
        val savedIndex = viewModel.saltMixProduct.value ?: 0
        if (products.isNotEmpty()) {
            spinner.setText(products.getOrElse(savedIndex) { products[0] }, false)
        }
        
        val savedVol = viewModel.saltMixVolume.value
        if (savedVol != null && savedVol > 0) etVol.setText(savedVol.toString())
        
        val savedCur = viewModel.saltMixCurrentPpt.value
        if (savedCur != null && savedCur > 0) etCur.setText(savedCur.toString())
        
        val savedTar = viewModel.saltMixDesiredPpt.value
        if (savedTar != null && savedTar > 0) etTar.setText(savedTar.toString())

        fun calculate() {
            // Get inputs
            val vol = etVol.text?.toString()?.toDoubleOrNull() ?: 0.0
            val cur = etCur.text?.toString()?.toDoubleOrNull() ?: 0.0
            val tar = etTar.text?.toString()?.toDoubleOrNull() ?: 0.0
            val prodName = spinner.text.toString()
            val prodIndex = products.indexOf(prodName).coerceAtLeast(0)

            // Update VM
            viewModel.setSaltMixProduct(prodIndex)
            viewModel.setSaltMixVolume(vol)
            viewModel.setSaltMixCurrentPpt(cur)
            viewModel.setSaltMixDesiredPpt(tar)

            // Calculate
            val res = viewModel.calculateSaltMix()
            if (res != null) {
                 resultPrimary.text = "${res.kilograms} kg"
                 resultDetails.text = "${res.grams} g (${res.pounds} lbs)"
            } else {
                 resultPrimary.text = "---"
                 resultDetails.text = "Enter valid inputs (Target > Current)"
            }
        }

        // Use debounced watchers
        val volWatcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        val curWatcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }
        val tarWatcher = DebouncedTextWatcher(INPUT_DEBOUNCE_MS, viewLifecycleOwner) { calculate() }

        etVol.addTextChangedListener(volWatcher)
        etCur.addTextChangedListener(curWatcher)
        etTar.addTextChangedListener(tarWatcher)
        spinner.setOnItemClickListener { _, _, _, _ -> calculate() }

        calculate()
    }

    private fun applyProfileUi() {
        val profile = viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER
        binding.groupCalcFreshwater.visibility = if (profile == MainViewModel.AquariumProfile.FRESHWATER) View.VISIBLE else View.GONE
        binding.groupCalcSaltwater.visibility = if (profile == MainViewModel.AquariumProfile.SALTWATER) View.VISIBLE else View.GONE
        binding.groupCalcPond.visibility = if (profile == MainViewModel.AquariumProfile.POND) View.VISIBLE else View.GONE

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
        binding.tvProfileBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), badgeColor))
    }

    private fun observeViewModel() {
        viewModel.volume.observe(viewLifecycleOwner) { updateVolumeDisplay() }
        viewModel.profile.observe(viewLifecycleOwner) { applyProfileUi() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up text watchers to prevent memory leaks
        textWatcherManager.cleanup()
        _binding = null
    }
}
