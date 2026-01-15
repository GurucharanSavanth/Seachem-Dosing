package com.example.seachem_dosing.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ai.ChatMessage
import com.example.seachem_dosing.ai.ChatRole
import com.example.seachem_dosing.databinding.FragmentDashboardBinding
import com.example.seachem_dosing.databinding.ItemParameterBinding
import com.example.seachem_dosing.databinding.ItemParameterHardnessBinding
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.MaterialFadeThrough

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var ghBinding: ItemParameterHardnessBinding? = null
    private var khBinding: ItemParameterHardnessBinding? = null
    private var isGhUpdating = false
    private var isKhUpdating = false
    private val logTag = "DashboardFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVolumeSection()
        applyProfileUi()
        setupParameters()
        setupRecommendationsMenu()
        setupAiSection()
        observeViewModel()
        updateRecommendations()
    }

    private fun setupVolumeSection() {
        val currentMode = viewModel.volumeMode.value ?: "direct"
        // Volume mode toggle
        binding.toggleVolumeMode.check(if (currentMode == "lbh") R.id.btnLbh else R.id.btnDirect)
        binding.layoutDirect.visibility = if (currentMode == "lbh") View.GONE else View.VISIBLE
        binding.layoutDimensions.visibility = if (currentMode == "lbh") View.VISIBLE else View.GONE
        binding.toggleVolumeMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnDirect -> {
                        binding.layoutDirect.visibility = View.VISIBLE
                        binding.layoutDimensions.visibility = View.GONE
                        viewModel.setVolumeMode("direct")
                    }
                    R.id.btnLbh -> {
                        binding.layoutDirect.visibility = View.GONE
                        binding.layoutDimensions.visibility = View.VISIBLE
                        viewModel.setVolumeMode("lbh")
                    }
                }
                updateCalculatedVolume()
            }
        }

        // Volume unit dropdown
        val volumeUnits = getVolumeUnitOptions()
        val volumeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, volumeUnits)
        binding.spinnerVolumeUnit.setAdapter(volumeAdapter)
        val initialVolumeUnit = when (viewModel.volumeUnit.value ?: "US") {
            "L" -> 1
            "UK" -> 2
            else -> 0
        }
        binding.spinnerVolumeUnit.setText(volumeUnits[initialVolumeUnit], false)
        binding.spinnerVolumeUnit.setOnItemClickListener { _, _, position, _ ->
            val unit = when (position) {
                0 -> "US"
                1 -> "L"
                2 -> "UK"
                else -> "US"
            }
            viewModel.setVolumeUnit(unit)
            updateRecommendations()
        }

        // Dimension unit dropdown
        val dimUnits = getDimUnitOptions()
        val dimAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dimUnits)
        binding.spinnerDimUnit.setAdapter(dimAdapter)
        val initialDimUnit = when (viewModel.dimUnit.value ?: "cm") {
            "in" -> 1
            "ft" -> 2
            else -> 0
        }
        binding.spinnerDimUnit.setText(dimUnits[initialDimUnit], false)
        binding.spinnerDimUnit.setOnItemClickListener { _, _, position, _ ->
            viewModel.setDimUnit(dimUnits[position])
            updateCalculatedVolume()
        }

        // Volume input
        binding.etVolume.setText(viewModel.volume.value?.toString() ?: "10")
        binding.etVolume.addTextChangedListener(createTextWatcher { value ->
            viewModel.setVolume(value)
            updateRecommendations()
        })

        // Dimension inputs
        binding.etLength.setText(viewModel.dimLength.value?.toString() ?: "60")
        binding.etBreadth.setText(viewModel.dimBreadth.value?.toString() ?: "30")
        binding.etHeight.setText(viewModel.dimHeight.value?.toString() ?: "40")

        binding.etLength.addTextChangedListener(createTextWatcher { value ->
            viewModel.setDimLength(value)
            updateCalculatedVolume()
        })
        binding.etBreadth.addTextChangedListener(createTextWatcher { value ->
            viewModel.setDimBreadth(value)
            updateCalculatedVolume()
        })
        binding.etHeight.addTextChangedListener(createTextWatcher { value ->
            viewModel.setDimHeight(value)
            updateCalculatedVolume()
        })

        updateCalculatedVolume()
    }

    private fun updateCalculatedVolume() {
        val litres = viewModel.getEffectiveVolumeLitres()
        binding.tvCalculatedVolume.text = getString(R.string.calculated_volume, String.format("%.1f", litres))
        updateRecommendations()
    }

    private fun setupParameters() {
        when (viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER) {
            MainViewModel.AquariumProfile.FRESHWATER -> setupFreshwaterParameters()
            MainViewModel.AquariumProfile.SALTWATER -> setupSaltwaterParameters()
            MainViewModel.AquariumProfile.POND -> setupPondParameters()
        }
    }

    private fun applyProfileUi() {
        val profile = viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER
        binding.groupFreshwater.visibility = if (profile == MainViewModel.AquariumProfile.FRESHWATER) View.VISIBLE else View.GONE
        binding.groupSaltwater.visibility = if (profile == MainViewModel.AquariumProfile.SALTWATER) View.VISIBLE else View.GONE
        binding.groupPond.visibility = if (profile == MainViewModel.AquariumProfile.POND) View.VISIBLE else View.GONE

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

    private fun setupFreshwaterParameters() {
        // Ammonia
        val ammoniaBinding = ItemParameterBinding.bind(binding.paramAmmonia.root)
        ammoniaBinding.tvParamName.text = getString(R.string.param_ammonia)
        ammoniaBinding.tvUnit.text = getString(R.string.unit_ppm)
        ammoniaBinding.etValue.setText(viewModel.ammonia.value?.toString() ?: "0")
        ammoniaBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setAmmonia(value)
            updateParameterStatus(ammoniaBinding, viewModel.getAmmoniaStatus())
            updateRecommendations()
        })
        updateParameterStatus(ammoniaBinding, viewModel.getAmmoniaStatus())

        // Nitrite
        val nitriteBinding = ItemParameterBinding.bind(binding.paramNitrite.root)
        nitriteBinding.tvParamName.text = getString(R.string.param_nitrite)
        nitriteBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitriteBinding.etValue.setText(viewModel.nitrite.value?.toString() ?: "0")
        nitriteBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrite(value)
            updateParameterStatus(nitriteBinding, viewModel.getNitriteStatus())
            updateRecommendations()
        })
        updateParameterStatus(nitriteBinding, viewModel.getNitriteStatus())

        // Nitrate
        val nitrateBinding = ItemParameterBinding.bind(binding.paramNitrate.root)
        nitrateBinding.tvParamName.text = getString(R.string.param_nitrate)
        nitrateBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitrateBinding.etValue.setText(viewModel.nitrate.value?.toString() ?: "15")
        nitrateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrate(value)
            updateParameterStatus(nitrateBinding, viewModel.getNitrateStatus())
            updateRecommendations()
        })
        updateParameterStatus(nitrateBinding, viewModel.getNitrateStatus())

        // GH
        ghBinding = ItemParameterHardnessBinding.bind(binding.paramGh.root)
        ghBinding?.tvParamName?.text = getString(R.string.param_gh)
        ghBinding?.btnDh?.text = getString(R.string.unit_dgh_short)
        ghBinding?.btnPpm?.text = getString(R.string.unit_ppm_caps)
        ghBinding?.etValue?.setText(viewModel.gh.value?.toString() ?: "4")
        applyHardnessToggleState(ghBinding, viewModel.ghUnit.value ?: "dh")
        ghBinding?.toggleUnit?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newUnit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                val oldUnit = viewModel.ghUnit.value ?: "dh"
                if (newUnit != oldUnit) {
                    val currentValue = ghBinding?.etValue?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val convertedValue = convertHardnessValue(currentValue, oldUnit, newUnit)
                    isGhUpdating = true
                    ghBinding?.etValue?.setText(formatHardnessInput(convertedValue, newUnit))
                    isGhUpdating = false
                    viewModel.setGhUnit(newUnit)
                    viewModel.setGh(convertedValue)
                    viewModel.syncGhFromParams()
                    Log.d(logTag, "GH unit changed $oldUnit -> $newUnit, $currentValue -> $convertedValue")
                }
                updateRecommendations()
            }
        }
        ghBinding?.etValue?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isGhUpdating) return
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.setGh(value)
                viewModel.syncGhFromParams()
                updateRecommendations()
            }
        })

        // KH
        khBinding = ItemParameterHardnessBinding.bind(binding.paramKh.root)
        khBinding?.tvParamName?.text = getString(R.string.param_kh)
        khBinding?.btnDh?.text = getString(R.string.unit_dkh_short)
        khBinding?.btnPpm?.text = getString(R.string.unit_ppm_caps)
        khBinding?.etValue?.setText(viewModel.kh.value?.toString() ?: "6")
        applyHardnessToggleState(khBinding, viewModel.khUnit.value ?: "dh")
        khBinding?.toggleUnit?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newUnit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                val oldUnit = viewModel.khUnit.value ?: "dh"
                if (newUnit != oldUnit) {
                    val currentValue = khBinding?.etValue?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val convertedValue = convertHardnessValue(currentValue, oldUnit, newUnit)
                    isKhUpdating = true
                    khBinding?.etValue?.setText(formatHardnessInput(convertedValue, newUnit))
                    isKhUpdating = false
                    viewModel.setKhUnit(newUnit)
                    viewModel.setKh(convertedValue)
                    viewModel.syncKhFromParams()
                    Log.d(logTag, "KH unit changed $oldUnit -> $newUnit, $currentValue -> $convertedValue")
                }
                updateRecommendations()
            }
        }
        khBinding?.etValue?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isKhUpdating) return
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.setKh(value)
                viewModel.syncKhFromParams()
                updateRecommendations()
            }
        })

        // pH
        val phBinding = ItemParameterBinding.bind(binding.paramPh.root)
        phBinding.tvParamName.text = getString(R.string.param_ph)
        phBinding.tvUnit.text = getString(R.string.unit_ph)
        phBinding.etValue.setText(viewModel.ph.value?.toString() ?: "7.2")
        phBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPh(value)
            viewModel.syncPhFromParams()
            setStatusIndicator(phBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(phBinding, MainViewModel.Status.INFO)

        // Temperature
        val tempBinding = ItemParameterBinding.bind(binding.paramTemp.root)
        tempBinding.tvParamName.text = getString(R.string.param_temp)
        tempBinding.tvUnit.text = getString(R.string.unit_temp_c)
        tempBinding.etValue.setText(viewModel.temperature.value?.toString() ?: "26")
        tempBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setTemperature(value)
            setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(tempBinding, MainViewModel.Status.INFO)

        viewModel.syncGhFromParams()
        viewModel.syncKhFromParams()
        viewModel.syncPhFromParams()
    }

    private fun setupSaltwaterParameters() {
        ghBinding = null
        khBinding = null

        val salinityBinding = ItemParameterBinding.bind(binding.paramSalinity.root)
        salinityBinding.tvParamName.text = getString(R.string.param_salinity)
        salinityBinding.tvUnit.text = getString(R.string.unit_salinity_ppt)
        salinityBinding.etValue.setText(viewModel.salinity.value?.toString() ?: "35")
        salinityBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setSalinity(value)
            setStatusIndicator(salinityBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(salinityBinding, MainViewModel.Status.INFO)

        val alkalinityBinding = ItemParameterBinding.bind(binding.paramAlkalinity.root)
        alkalinityBinding.tvParamName.text = getString(R.string.param_alkalinity)
        alkalinityBinding.tvUnit.text = getString(R.string.unit_dkh)
        alkalinityBinding.etValue.setText(viewModel.alkalinity.value?.toString() ?: "8")
        alkalinityBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setAlkalinity(value)
            setStatusIndicator(alkalinityBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(alkalinityBinding, MainViewModel.Status.INFO)

        val calciumBinding = ItemParameterBinding.bind(binding.paramCalcium.root)
        calciumBinding.tvParamName.text = getString(R.string.param_calcium)
        calciumBinding.tvUnit.text = getString(R.string.unit_calcium_ppm)
        calciumBinding.etValue.setText(viewModel.calcium.value?.toString() ?: "420")
        calciumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setCalcium(value)
            setStatusIndicator(calciumBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(calciumBinding, MainViewModel.Status.INFO)

        val magnesiumBinding = ItemParameterBinding.bind(binding.paramMagnesium.root)
        magnesiumBinding.tvParamName.text = getString(R.string.param_magnesium)
        magnesiumBinding.tvUnit.text = getString(R.string.unit_magnesium_ppm)
        magnesiumBinding.etValue.setText(viewModel.magnesium.value?.toString() ?: "1300")
        magnesiumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setMagnesium(value)
            setStatusIndicator(magnesiumBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(magnesiumBinding, MainViewModel.Status.INFO)

        val nitrateBinding = ItemParameterBinding.bind(binding.paramNitrateSalt.root)
        nitrateBinding.tvParamName.text = getString(R.string.param_nitrate)
        nitrateBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitrateBinding.etValue.setText(viewModel.nitrate.value?.toString() ?: "10")
        nitrateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrate(value)
            setStatusIndicator(nitrateBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(nitrateBinding, MainViewModel.Status.INFO)

        val phosphateBinding = ItemParameterBinding.bind(binding.paramPhosphate.root)
        phosphateBinding.tvParamName.text = getString(R.string.param_phosphate)
        phosphateBinding.tvUnit.text = getString(R.string.unit_phosphate_ppm)
        phosphateBinding.etValue.setText(viewModel.phosphate.value?.toString() ?: "0.05")
        phosphateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPhosphate(value)
            setStatusIndicator(phosphateBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(phosphateBinding, MainViewModel.Status.INFO)

        val phBinding = ItemParameterBinding.bind(binding.paramPhSalt.root)
        phBinding.tvParamName.text = getString(R.string.param_ph)
        phBinding.tvUnit.text = getString(R.string.unit_ph)
        phBinding.etValue.setText(viewModel.ph.value?.toString() ?: "8.2")
        phBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPh(value)
            viewModel.syncPhFromParams()
            setStatusIndicator(phBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(phBinding, MainViewModel.Status.INFO)

        val tempBinding = ItemParameterBinding.bind(binding.paramTempSalt.root)
        tempBinding.tvParamName.text = getString(R.string.param_temp)
        tempBinding.tvUnit.text = getString(R.string.unit_temp_c)
        tempBinding.etValue.setText(viewModel.temperature.value?.toString() ?: "26")
        tempBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setTemperature(value)
            setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
    }

    private fun setupPondParameters() {
        ghBinding = null

        val ammoniaBinding = ItemParameterBinding.bind(binding.paramAmmoniaPond.root)
        ammoniaBinding.tvParamName.text = getString(R.string.param_ammonia)
        ammoniaBinding.tvUnit.text = getString(R.string.unit_ppm)
        ammoniaBinding.etValue.setText(viewModel.ammonia.value?.toString() ?: "0")
        ammoniaBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setAmmonia(value)
            updateParameterStatus(ammoniaBinding, viewModel.getAmmoniaStatus())
            updateRecommendations()
        })
        updateParameterStatus(ammoniaBinding, viewModel.getAmmoniaStatus())

        val nitriteBinding = ItemParameterBinding.bind(binding.paramNitritePond.root)
        nitriteBinding.tvParamName.text = getString(R.string.param_nitrite)
        nitriteBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitriteBinding.etValue.setText(viewModel.nitrite.value?.toString() ?: "0")
        nitriteBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrite(value)
            updateParameterStatus(nitriteBinding, viewModel.getNitriteStatus())
            updateRecommendations()
        })
        updateParameterStatus(nitriteBinding, viewModel.getNitriteStatus())

        val nitrateBinding = ItemParameterBinding.bind(binding.paramNitratePond.root)
        nitrateBinding.tvParamName.text = getString(R.string.param_nitrate)
        nitrateBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitrateBinding.etValue.setText(viewModel.nitrate.value?.toString() ?: "20")
        nitrateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrate(value)
            updateParameterStatus(nitrateBinding, viewModel.getNitrateStatus())
            updateRecommendations()
        })
        updateParameterStatus(nitrateBinding, viewModel.getNitrateStatus())

        khBinding = ItemParameterHardnessBinding.bind(binding.paramKhPond.root)
        khBinding?.tvParamName?.text = getString(R.string.param_kh)
        khBinding?.btnDh?.text = getString(R.string.unit_dkh_short)
        khBinding?.btnPpm?.text = getString(R.string.unit_ppm_caps)
        khBinding?.etValue?.setText(viewModel.kh.value?.toString() ?: "5")
        applyHardnessToggleState(khBinding, viewModel.khUnit.value ?: "dh")
        khBinding?.toggleUnit?.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newUnit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                val oldUnit = viewModel.khUnit.value ?: "dh"
                if (newUnit != oldUnit) {
                    val currentValue = khBinding?.etValue?.text?.toString()?.toDoubleOrNull() ?: 0.0
                    val convertedValue = convertHardnessValue(currentValue, oldUnit, newUnit)
                    isKhUpdating = true
                    khBinding?.etValue?.setText(formatHardnessInput(convertedValue, newUnit))
                    isKhUpdating = false
                    viewModel.setKhUnit(newUnit)
                    viewModel.setKh(convertedValue)
                    viewModel.syncKhFromParams()
                }
                updateRecommendations()
            }
        }
        khBinding?.etValue?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isKhUpdating) return
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.setKh(value)
                viewModel.syncKhFromParams()
                updateRecommendations()
            }
        })

        val phBinding = ItemParameterBinding.bind(binding.paramPhPond.root)
        phBinding.tvParamName.text = getString(R.string.param_ph)
        phBinding.tvUnit.text = getString(R.string.unit_ph)
        phBinding.etValue.setText(viewModel.ph.value?.toString() ?: "7.4")
        phBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPh(value)
            viewModel.syncPhFromParams()
            setStatusIndicator(phBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(phBinding, MainViewModel.Status.INFO)

        val tempBinding = ItemParameterBinding.bind(binding.paramTempPond.root)
        tempBinding.tvParamName.text = getString(R.string.param_temp)
        tempBinding.tvUnit.text = getString(R.string.unit_temp_c)
        tempBinding.etValue.setText(viewModel.temperature.value?.toString() ?: "22")
        tempBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setTemperature(value)
            setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(tempBinding, MainViewModel.Status.INFO)

        val oxygenBinding = ItemParameterBinding.bind(binding.paramOxygenPond.root)
        oxygenBinding.tvParamName.text = getString(R.string.param_oxygen)
        oxygenBinding.tvUnit.text = getString(R.string.unit_oxygen_mg_l)
        oxygenBinding.etValue.setText(viewModel.dissolvedOxygen.value?.toString() ?: "7.5")
        oxygenBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setDissolvedOxygen(value)
            setStatusIndicator(oxygenBinding, MainViewModel.Status.INFO)
            updateRecommendations()
        })
        setStatusIndicator(oxygenBinding, MainViewModel.Status.INFO)

        viewModel.syncKhFromParams()
        viewModel.syncPhFromParams()
    }

    private fun updateFreshwaterRecommendations() {
        val litres = viewModel.getEffectiveVolumeLitres()
        val ammonia = viewModel.ammonia.value ?: 0.0
        val nitrite = viewModel.nitrite.value ?: 0.0
        val nitrate = viewModel.nitrate.value ?: 0.0
        val gh = viewModel.getGhInDegrees()
        val kh = viewModel.getKhInDegrees()

        val recommendations = StringBuilder()

        if (litres <= 0) {
            recommendations.append(getString(R.string.reco_enter_volume))
        } else {
            if (ammonia > 0) {
                val primeDose = viewModel.calculatePrimeDose()
                recommendations.append(String.format(getString(R.string.reco_ammonia), primeDose))
                recommendations.append("\n\n")
            }
            if (nitrite > 0) {
                val stabilityDose = viewModel.calculateStabilityDose()
                recommendations.append(String.format(getString(R.string.reco_nitrite), stabilityDose))
                recommendations.append("\n\n")
            }
            if (nitrate > 50) {
                recommendations.append(getString(R.string.reco_nitrate))
                recommendations.append("\n\n")
            }
            if (gh < 3) {
                recommendations.append(getString(R.string.reco_gh_low))
                recommendations.append("\n\n")
            }
            if (kh < 3) {
                recommendations.append(getString(R.string.reco_kh_low))
                recommendations.append("\n\n")
            }
            if (recommendations.isEmpty()) {
                recommendations.append(getString(R.string.reco_ok))
            }
        }

        binding.tvRecommendations.text = recommendations.toString().trim()

        ghBinding?.let { updateHardnessStatus(it, viewModel.getGhInDegrees()) }
        khBinding?.let { updateHardnessStatus(it, viewModel.getKhInDegrees()) }
    }

    private fun updateSaltwaterRecommendations() {
        val litres = viewModel.getEffectiveVolumeLitres()
        val nitrate = viewModel.nitrate.value ?: 0.0
        val phosphate = viewModel.phosphate.value ?: 0.0

        val recommendations = StringBuilder()

        if (litres <= 0) {
            recommendations.append(getString(R.string.reco_enter_volume))
        } else {
            if (nitrate > 20) {
                recommendations.append(getString(R.string.reco_nitrate_salt))
                recommendations.append("\n\n")
            }
            if (phosphate > 0.1) {
                recommendations.append(getString(R.string.reco_phosphate_high))
                recommendations.append("\n\n")
            }
            if (recommendations.isEmpty()) {
                recommendations.append(getString(R.string.reco_ok))
            }
        }

        binding.tvRecommendations.text = recommendations.toString().trim()
    }

    private fun updatePondRecommendations() {
        val litres = viewModel.getEffectiveVolumeLitres()
        val ammonia = viewModel.ammonia.value ?: 0.0
        val nitrite = viewModel.nitrite.value ?: 0.0
        val nitrate = viewModel.nitrate.value ?: 0.0
        val kh = viewModel.getKhInDegrees()

        val recommendations = StringBuilder()

        if (litres <= 0) {
            recommendations.append(getString(R.string.reco_enter_volume))
        } else {
            if (ammonia > 0) {
                val primeDose = viewModel.calculatePrimeDose()
                recommendations.append(String.format(getString(R.string.reco_ammonia), primeDose))
                recommendations.append("\n\n")
            }
            if (nitrite > 0) {
                val stabilityDose = viewModel.calculateStabilityDose()
                recommendations.append(String.format(getString(R.string.reco_nitrite), stabilityDose))
                recommendations.append("\n\n")
            }
            if (nitrate > 50) {
                recommendations.append(getString(R.string.reco_nitrate))
                recommendations.append("\n\n")
            }
            if (kh < 3) {
                recommendations.append(getString(R.string.reco_kh_low))
                recommendations.append("\n\n")
            }
            if (recommendations.isEmpty()) {
                recommendations.append(getString(R.string.reco_ok))
            }
        }

        binding.tvRecommendations.text = recommendations.toString().trim()
        khBinding?.let { updateHardnessStatus(it, viewModel.getKhInDegrees()) }
    }

    private fun updateParameterStatus(binding: ItemParameterBinding, status: MainViewModel.ParameterStatus) {
        val color = when (status.status) {
            MainViewModel.Status.GOOD -> R.color.status_good
            MainViewModel.Status.WARNING -> R.color.status_warning
            MainViewModel.Status.DANGER -> R.color.status_danger
            MainViewModel.Status.INFO -> R.color.status_info
        }
        val drawable = binding.statusIndicator.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(requireContext(), color))
    }

    private fun setStatusIndicator(binding: ItemParameterBinding, status: MainViewModel.Status) {
        updateParameterStatus(binding, MainViewModel.ParameterStatus(status, ""))
    }

    private fun updateRecommendations() {
        when (viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER) {
            MainViewModel.AquariumProfile.FRESHWATER -> updateFreshwaterRecommendations()
            MainViewModel.AquariumProfile.SALTWATER -> updateSaltwaterRecommendations()
            MainViewModel.AquariumProfile.POND -> updatePondRecommendations()
        }
    }

    private fun observeViewModel() {
        viewModel.ghUnit.observe(viewLifecycleOwner) { unit ->
            applyHardnessToggleState(ghBinding, unit)
            ghBinding?.etValue?.let { editText ->
                if (!editText.isFocused) {
                    isGhUpdating = true
                    editText.setText(formatHardnessInput(viewModel.gh.value ?: 0.0, unit))
                    isGhUpdating = false
                }
            }
            updateRecommendations()
        }
        viewModel.khUnit.observe(viewLifecycleOwner) { unit ->
            applyHardnessToggleState(khBinding, unit)
            khBinding?.etValue?.let { editText ->
                if (!editText.isFocused) {
                    isKhUpdating = true
                    editText.setText(formatHardnessInput(viewModel.kh.value ?: 0.0, unit))
                    isKhUpdating = false
                }
            }
            updateRecommendations()
        }
        viewModel.volumeUnit.observe(viewLifecycleOwner) { unit ->
            val volumeUnits = getVolumeUnitOptions()
            val index = when (unit) {
                "L" -> 1
                "UK" -> 2
                else -> 0
            }
            binding.spinnerVolumeUnit.setText(volumeUnits[index], false)
            updateCalculatedVolume()
        }
        viewModel.profile.observe(viewLifecycleOwner) {
            applyProfileUi()
        }
        viewModel.aiInsight.observe(viewLifecycleOwner) { state ->
            binding.progressAiInsight.isVisible = state.isLoading
            binding.btnAiInsight.isEnabled = !state.isLoading
            binding.tvAiInsightError.isVisible = !state.error.isNullOrBlank()
            binding.tvAiInsightError.text = state.error ?: ""
            binding.tvAiInsight.text = state.text ?: getString(R.string.ai_insight_placeholder)
        }
        viewModel.chatMessages.observe(viewLifecycleOwner) { messages ->
            renderChatMessages(messages)
        }
    }

    private fun createTextWatcher(onTextChanged: (Double) -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                onTextChanged(value)
            }
        }
    }

    private fun applyHardnessToggleState(binding: ItemParameterHardnessBinding?, unit: String) {
        binding?.toggleUnit?.check(if (unit == "ppm") R.id.btnPpm else R.id.btnDh)
    }

    private fun getVolumeUnitOptions(): Array<String> {
        return arrayOf(
            getString(R.string.unit_us_gallon),
            getString(R.string.unit_litre),
            getString(R.string.unit_uk_gallon)
        )
    }

    private fun getDimUnitOptions(): Array<String> {
        return arrayOf(
            getString(R.string.unit_cm),
            getString(R.string.unit_in),
            getString(R.string.unit_ft)
        )
    }

    private fun updateHardnessStatus(binding: ItemParameterHardnessBinding, valueDh: Double) {
        val statusColor = if (valueDh in 0.1..2.99) R.color.status_warning else R.color.status_info
        val drawable = binding.statusIndicator.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(requireContext(), statusColor))
    }

    private fun convertHardnessValue(value: Double, fromUnit: String, toUnit: String): Double {
        return when {
            fromUnit == toUnit -> value
            fromUnit == "ppm" && toUnit == "dh" -> Calculations.ppmToDh(value)
            fromUnit == "dh" && toUnit == "ppm" -> Calculations.dhToPpm(value)
            else -> value
        }
    }

    private fun formatHardnessInput(value: Double, unit: String): String {
        return if (unit == "ppm") {
            String.format("%.0f", value)
        } else {
            String.format("%.1f", value)
        }
    }

    private fun setupRecommendationsMenu() {
        binding.btnRecommendationsMenu.setOnClickListener {
            showRecommendationsMenu(it)
        }
        binding.cardRecommendations.setOnLongClickListener {
            showRecommendationsMenu(binding.btnRecommendationsMenu)
            true
        }
    }

    private fun setupAiSection() {
        binding.btnAiInsight.setOnClickListener {
            viewModel.requestAiInsight()
        }
        binding.btnChatSend.setOnClickListener {
            sendChatMessage()
        }
        binding.etChatInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendChatMessage()
                true
            } else {
                false
            }
        }
    }

    private fun sendChatMessage() {
        val message = binding.etChatInput.text?.toString().orEmpty()
        if (message.isBlank()) return
        binding.etChatInput.setText("")
        viewModel.sendChatMessage(message)
    }

    private fun renderChatMessages(messages: List<ChatMessage>) {
        val container = binding.chatMessagesContainer
        container.removeAllViews()
        messages.forEach { message ->
            val item = layoutInflater.inflate(R.layout.item_chat_message, container, false) as LinearLayout
            val textView = item.findViewById<android.widget.TextView>(R.id.tvMessage)
            textView.text = message.text
            if (message.role == ChatRole.USER) {
                item.gravity = android.view.Gravity.END
                textView.setBackgroundResource(R.drawable.bg_chat_user)
                textView.setTextColor(
                    MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOnPrimary)
                )
            } else {
                item.gravity = android.view.Gravity.START
                textView.setBackgroundResource(R.drawable.bg_chat_assistant)
                textView.setTextColor(
                    MaterialColors.getColor(textView, com.google.android.material.R.attr.colorOnSurface)
                )
            }
            container.addView(item)
        }
        binding.chatScroll.post {
            binding.chatScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRecommendationsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.context_recommendations, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_copy_recommendations -> {
                    copyRecommendations()
                    true
                }
                R.id.action_share_recommendations -> {
                    shareRecommendations()
                    true
                }
                R.id.action_switch_profile -> {
                    val navController = findNavController()
                    val popped = navController.popBackStack(R.id.navigation_profile, false)
                    if (!popped) {
                        navController.navigate(R.id.navigation_profile)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun copyRecommendations() {
        val text = binding.tvRecommendations.text?.toString().orEmpty()
        val clipboard = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(getString(R.string.section_recommendations), text))
        Toast.makeText(requireContext(), getString(R.string.action_copied), Toast.LENGTH_SHORT).show()
    }

    private fun shareRecommendations() {
        val text = binding.tvRecommendations.text?.toString().orEmpty()
        if (text.isBlank()) return
        val intent = ShareCompat.IntentBuilder(requireActivity())
            .setType("text/plain")
            .setText(text)
            .setChooserTitle(getString(R.string.share_recommendations_title))
            .createChooserIntent()
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        applyProfileUi()
        val ghUnit = viewModel.ghUnit.value ?: "dh"
        val khUnit = viewModel.khUnit.value ?: "dh"
        applyHardnessToggleState(ghBinding, ghUnit)
        applyHardnessToggleState(khBinding, khUnit)
        val volumeUnits = getVolumeUnitOptions()
        val volumeIndex = when (viewModel.volumeUnit.value ?: "US") {
            "L" -> 1
            "UK" -> 2
            else -> 0
        }
        binding.spinnerVolumeUnit.setText(volumeUnits[volumeIndex], false)
        val dimUnits = getDimUnitOptions()
        val dimIndex = when (viewModel.dimUnit.value ?: "cm") {
            "in" -> 1
            "ft" -> 2
            else -> 0
        }
        binding.spinnerDimUnit.setText(dimUnits[dimIndex], false)
        ghBinding?.etValue?.let { editText ->
            if (!editText.isFocused) {
                isGhUpdating = true
                editText.setText(formatHardnessInput(viewModel.gh.value ?: 0.0, ghUnit))
                isGhUpdating = false
            }
        }
        khBinding?.etValue?.let { editText ->
            if (!editText.isFocused) {
                isKhUpdating = true
                editText.setText(formatHardnessInput(viewModel.kh.value ?: 0.0, khUnit))
                isKhUpdating = false
            }
        }
        updateRecommendations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        ghBinding = null
        khBinding = null
    }
}
