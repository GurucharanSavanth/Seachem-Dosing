package com.example.seachem_dosing.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.FragmentDashboardBinding
import com.example.seachem_dosing.databinding.ItemParameterBinding
import com.example.seachem_dosing.databinding.ItemParameterHardnessBinding
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.transition.MaterialFadeThrough

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var ghBinding: ItemParameterHardnessBinding? = null
    private var khBinding: ItemParameterHardnessBinding? = null
    private var ghFreshwaterBinding: ItemParameterHardnessBinding? = null
    private var khFreshwaterBinding: ItemParameterHardnessBinding? = null
    // Removed khPondBinding
    private val configuredProfiles = mutableSetOf<MainViewModel.AquariumProfile>()
    private val recommendationsHandler = Handler(Looper.getMainLooper())
    private val recommendationsUpdateDelayMs = 200L
    private val recommendationsRunnable = Runnable { updateRecommendations() }
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
        setupRecommendationsMenu()
        val currentProfile = viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER
        setupParameters(currentProfile)
        applyProfileUi(currentProfile)
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
            scheduleRecommendationsUpdate()
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
            scheduleRecommendationsUpdate()
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
        scheduleRecommendationsUpdate()
    }

    private fun setupParameters(profile: MainViewModel.AquariumProfile) {
        if (configuredProfiles.add(profile)) {
            when (profile) {
                MainViewModel.AquariumProfile.FRESHWATER -> setupFreshwaterParameters()
                MainViewModel.AquariumProfile.SALTWATER -> setupSaltwaterParameters()
                MainViewModel.AquariumProfile.POND -> { /* No parameters for Utility profile */ }
            }
        }
        updateActiveHardnessBindings(profile)
    }

    private fun applyProfileUi(profile: MainViewModel.AquariumProfile) {
        val isUtility = profile == MainViewModel.AquariumProfile.POND
        
        binding.groupFreshwater.visibility = if (profile == MainViewModel.AquariumProfile.FRESHWATER) View.VISIBLE else View.GONE
        binding.groupSaltwater.visibility = if (profile == MainViewModel.AquariumProfile.SALTWATER) View.VISIBLE else View.GONE
        binding.groupPond.visibility = View.GONE // Always hide, parameters removed
        
        // Hide "Water Parameters" header if utility profile
        binding.tvParametersHeader.visibility = if (isUtility) View.GONE else View.VISIBLE

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

        updateActiveHardnessBindings(profile)
        val ghUnit = viewModel.ghUnit.value ?: "dh"
        val khUnit = viewModel.khUnit.value ?: "dh"
        applyHardnessToggleState(ghBinding, ghUnit)
        applyHardnessToggleState(khBinding, khUnit)
        syncGhInput(ghUnit)
        syncKhInput(khUnit)
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
            scheduleRecommendationsUpdate()
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
            scheduleRecommendationsUpdate()
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
            scheduleRecommendationsUpdate()
        })
        updateParameterStatus(nitrateBinding, viewModel.getNitrateStatus())

        // GH
        val ghBindingLocal = ItemParameterHardnessBinding.bind(binding.paramGh.root)
        ghFreshwaterBinding = ghBindingLocal
        ghBindingLocal.tvParamName.text = getString(R.string.param_gh)
        ghBindingLocal.btnDh.text = getString(R.string.unit_dgh_short)
        ghBindingLocal.btnPpm.text = getString(R.string.unit_ppm_caps)
        ghBindingLocal.etValue.setText(viewModel.gh.value?.toString() ?: "4")
        applyHardnessToggleState(ghBindingLocal, viewModel.ghUnit.value ?: "dh")
        ghBindingLocal.toggleUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newUnit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                val oldUnit = viewModel.ghUnit.value ?: "dh"
                if (newUnit != oldUnit) {
                    val currentValue = viewModel.gh.value ?: 0.0
                    val convertedValue = convertHardnessValue(currentValue, oldUnit, newUnit)
                    isGhUpdating = true
                    ghBindingLocal.etValue.setText(formatHardnessInput(convertedValue, newUnit))
                    isGhUpdating = false
                    viewModel.setGh(convertedValue)
                    viewModel.setGhUnit(newUnit)
                    viewModel.syncGhFromParams()
                    Log.d(logTag, "GH unit changed $oldUnit -> $newUnit, $currentValue -> $convertedValue")
                }
                scheduleRecommendationsUpdate()
            }
        }
        ghBindingLocal.etValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isGhUpdating) return
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.setGh(value)
                viewModel.syncGhFromParams()
                scheduleRecommendationsUpdate()
            }
        })

        // KH
        val khBindingLocal = ItemParameterHardnessBinding.bind(binding.paramKh.root)
        khFreshwaterBinding = khBindingLocal
        khBindingLocal.tvParamName.text = getString(R.string.param_kh)
        khBindingLocal.btnDh.text = getString(R.string.unit_dkh_short)
        khBindingLocal.btnPpm.text = getString(R.string.unit_ppm_caps)
        khBindingLocal.etValue.setText(viewModel.kh.value?.toString() ?: "6")
        applyHardnessToggleState(khBindingLocal, viewModel.khUnit.value ?: "dh")
        khBindingLocal.toggleUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newUnit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                val oldUnit = viewModel.khUnit.value ?: "dh"
                if (newUnit != oldUnit) {
                    val currentValue = viewModel.kh.value ?: 0.0
                    val convertedValue = convertHardnessValue(currentValue, oldUnit, newUnit)
                    isKhUpdating = true
                    khBindingLocal.etValue.setText(formatHardnessInput(convertedValue, newUnit))
                    isKhUpdating = false
                    viewModel.setKh(convertedValue)
                    viewModel.setKhUnit(newUnit)
                    viewModel.syncKhFromParams()
                    Log.d(logTag, "KH unit changed $oldUnit -> $newUnit, $currentValue -> $convertedValue")
                }
                scheduleRecommendationsUpdate()
            }
        }
        khBindingLocal.etValue.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isKhUpdating) return
                val value = s?.toString()?.toDoubleOrNull() ?: 0.0
                viewModel.setKh(value)
                viewModel.syncKhFromParams()
                scheduleRecommendationsUpdate()
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
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(phBinding, MainViewModel.Status.INFO)
        
        // Potassium
        val potassiumBinding = ItemParameterBinding.bind(binding.paramPotassium.root)
        potassiumBinding.tvParamName.text = getString(R.string.param_potassium)
        potassiumBinding.tvUnit.text = getString(R.string.unit_mg_l)
        potassiumBinding.etValue.setText(viewModel.potassium.value?.toString() ?: "15")
        potassiumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPotassium(value)
            viewModel.syncNewParams()
            setStatusIndicator(potassiumBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(potassiumBinding, MainViewModel.Status.INFO)
        
        // Iron
        val ironBinding = ItemParameterBinding.bind(binding.paramIron.root)
        ironBinding.tvParamName.text = getString(R.string.param_iron)
        ironBinding.tvUnit.text = getString(R.string.unit_mg_l)
        ironBinding.etValue.setText(viewModel.iron.value?.toString() ?: "0.1")
        ironBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setIron(value)
            viewModel.syncNewParams()
            setStatusIndicator(ironBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(ironBinding, MainViewModel.Status.INFO)

        // Temperature
        val tempBinding = ItemParameterBinding.bind(binding.paramTemp.root)
        tempBinding.tvParamName.text = getString(R.string.param_temp)
        tempBinding.tvUnit.text = getString(R.string.unit_temp_c)
        tempBinding.etValue.setText(viewModel.temperature.value?.toString() ?: "26")
        tempBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setTemperature(value)
            setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(tempBinding, MainViewModel.Status.INFO)

        viewModel.syncGhFromParams()
        viewModel.syncKhFromParams()
        viewModel.syncPhFromParams()
        viewModel.syncNewParams()
    }

    private fun setupSaltwaterParameters() {
        val salinityBinding = ItemParameterBinding.bind(binding.paramSalinity.root)
        salinityBinding.tvParamName.text = getString(R.string.param_salinity)
        salinityBinding.tvUnit.text = getString(R.string.unit_salinity_ppt)
        salinityBinding.etValue.setText(viewModel.salinity.value?.toString() ?: "35")
        salinityBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setSalinity(value)
            setStatusIndicator(salinityBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(salinityBinding, MainViewModel.Status.INFO)

        val alkalinityBinding = ItemParameterBinding.bind(binding.paramAlkalinity.root)
        alkalinityBinding.tvParamName.text = getString(R.string.param_alkalinity)
        alkalinityBinding.tvUnit.text = getString(R.string.unit_dkh)
        alkalinityBinding.etValue.setText(viewModel.alkalinity.value?.toString() ?: "8")
        alkalinityBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setAlkalinity(value)
            setStatusIndicator(alkalinityBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(alkalinityBinding, MainViewModel.Status.INFO)

        val calciumBinding = ItemParameterBinding.bind(binding.paramCalcium.root)
        calciumBinding.tvParamName.text = getString(R.string.param_calcium)
        calciumBinding.tvUnit.text = getString(R.string.unit_calcium_ppm)
        calciumBinding.etValue.setText(viewModel.calcium.value?.toString() ?: "420")
        calciumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setCalcium(value)
            viewModel.syncNewParams()
            setStatusIndicator(calciumBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(calciumBinding, MainViewModel.Status.INFO)

        val magnesiumBinding = ItemParameterBinding.bind(binding.paramMagnesium.root)
        magnesiumBinding.tvParamName.text = getString(R.string.param_magnesium)
        magnesiumBinding.tvUnit.text = getString(R.string.unit_magnesium_ppm)
        magnesiumBinding.etValue.setText(viewModel.magnesium.value?.toString() ?: "1300")
        magnesiumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setMagnesium(value)
            viewModel.syncNewParams()
            setStatusIndicator(magnesiumBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(magnesiumBinding, MainViewModel.Status.INFO)

        val nitrateBinding = ItemParameterBinding.bind(binding.paramNitrateSalt.root)
        nitrateBinding.tvParamName.text = getString(R.string.param_nitrate)
        nitrateBinding.tvUnit.text = getString(R.string.unit_ppm)
        nitrateBinding.etValue.setText(viewModel.nitrate.value?.toString() ?: "10")
        nitrateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setNitrate(value)
            setStatusIndicator(nitrateBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(nitrateBinding, MainViewModel.Status.INFO)

        val phosphateBinding = ItemParameterBinding.bind(binding.paramPhosphate.root)
        phosphateBinding.tvParamName.text = getString(R.string.param_phosphate)
        phosphateBinding.tvUnit.text = getString(R.string.unit_phosphate_ppm)
        phosphateBinding.etValue.setText(viewModel.phosphate.value?.toString() ?: "0.05")
        phosphateBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setPhosphate(value)
            viewModel.syncNewParams()
            setStatusIndicator(phosphateBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
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
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(phBinding, MainViewModel.Status.INFO)
        
        // Strontium
        val strontiumBinding = ItemParameterBinding.bind(binding.paramStrontium.root)
        strontiumBinding.tvParamName.text = getString(R.string.param_strontium)
        strontiumBinding.tvUnit.text = getString(R.string.unit_mg_l)
        strontiumBinding.etValue.setText(viewModel.strontium.value?.toString() ?: "8.0")
        strontiumBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setStrontium(value)
            viewModel.syncNewParams()
            setStatusIndicator(strontiumBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(strontiumBinding, MainViewModel.Status.INFO)
        
        // Iodide
        val iodideBinding = ItemParameterBinding.bind(binding.paramIodide.root)
        iodideBinding.tvParamName.text = getString(R.string.param_iodide)
        iodideBinding.tvUnit.text = getString(R.string.unit_mg_l)
        iodideBinding.etValue.setText(viewModel.iodide.value?.toString() ?: "0.06")
        iodideBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setIodide(value)
            viewModel.syncNewParams()
            setStatusIndicator(iodideBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(iodideBinding, MainViewModel.Status.INFO)

        val tempBinding = ItemParameterBinding.bind(binding.paramTempSalt.root)
        tempBinding.tvParamName.text = getString(R.string.param_temp)
        tempBinding.tvUnit.text = getString(R.string.unit_temp_c)
        tempBinding.etValue.setText(viewModel.temperature.value?.toString() ?: "26")
        tempBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setTemperature(value)
            setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
            scheduleRecommendationsUpdate()
        })
        setStatusIndicator(tempBinding, MainViewModel.Status.INFO)
    }

    private fun updateFreshwaterRecommendations() {
        val litres = viewModel.getEffectiveVolumeLitres()
        val ammonia = viewModel.ammonia.value ?: 0.0
        val nitrite = viewModel.nitrite.value ?: 0.0
        val nitrate = viewModel.nitrate.value ?: 0.0
        val gh = viewModel.getGhInDegrees()
        val kh = viewModel.getKhInDegrees()
        val ph = viewModel.ph.value ?: 0.0
        val temp = viewModel.temperature.value ?: 0.0
        val k = viewModel.potassium.value ?: 0.0
        val fe = viewModel.iron.value ?: 0.0

        val actions = mutableListOf<String>()
        if (litres <= 0) {
            actions.add(getString(R.string.reco_action_volume_needed))
        }
        if (ammonia > 0) {
            if (litres > 0) {
                actions.add(getString(R.string.reco_action_ammonia, viewModel.calculatePrimeDose()))
            } else {
                actions.add(getString(R.string.reco_action_ammonia_generic))
            }
        }
        if (nitrite > 0) {
            if (litres > 0) {
                actions.add(getString(R.string.reco_action_nitrite, viewModel.calculateStabilityDose()))
            } else {
                actions.add(getString(R.string.reco_action_nitrite_generic))
            }
        }
        if (nitrate > 50) {
            if (litres > 0) {
                val changeLitres = formatValue(viewModel.calculateWaterChangeLitres(30.0), 1)
                actions.add(getString(R.string.reco_action_nitrate_high, changeLitres))
            } else {
                actions.add(getString(R.string.reco_action_nitrate_high_generic))
            }
        }
        if (kh < 3) {
            actions.add(getString(R.string.reco_action_kh_low))
        }
        
        // New recommendations
        if (k < 10) actions.add("Low Potassium: Consider dosing Flourish Potassium.")
        if (fe < 0.05) actions.add("Low Iron: Consider dosing Flourish Iron.")

        val details = mutableListOf<String>()
        details.add(
            buildRecommendationLine(
                R.string.param_ammonia,
                formatValueWithUnit(ammonia, 2, getString(R.string.unit_ppm)),
                if (ammonia > 0) R.string.reco_msg_ammonia_high else R.string.reco_msg_ammonia_ok
            )
        )
        details.add(
            buildRecommendationLine(
                R.string.param_nitrite,
                formatValueWithUnit(nitrite, 2, getString(R.string.unit_ppm)),
                if (nitrite > 0) R.string.reco_msg_nitrite_high else R.string.reco_msg_nitrite_ok
            )
        )
        val nitrateMessage = when {
            nitrate > 50 -> R.string.reco_msg_nitrate_high
            nitrate > 20 -> R.string.reco_msg_nitrate_moderate
            else -> R.string.reco_msg_nitrate_low
        }
        details.add(
            buildRecommendationLine(
                R.string.param_nitrate,
                formatValueWithUnit(nitrate, 0, getString(R.string.unit_ppm)),
                nitrateMessage
            )
        )
        val ghMessage = when {
            gh > 12 -> R.string.reco_msg_gh_high
            gh < 3 -> R.string.reco_msg_gh_low
            else -> R.string.reco_msg_gh_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_gh,
                formatValueWithUnit(gh, 1, getString(R.string.unit_dgh_short)),
                ghMessage
            )
        )
        val khMessage = when {
            kh > 10 -> R.string.reco_msg_kh_high
            kh < 3 -> R.string.reco_msg_kh_low
            else -> R.string.reco_msg_kh_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_kh,
                formatValueWithUnit(kh, 1, getString(R.string.unit_dkh_short)),
                khMessage
            )
        )
        val phMessage = when {
            ph < 6.5 -> R.string.reco_msg_ph_low
            ph > 8.2 -> R.string.reco_msg_ph_high
            else -> R.string.reco_msg_ph_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_ph,
                formatValueWithUnit(ph, 2, getString(R.string.unit_ph)),
                phMessage
            )
        )
        
        details.add(getString(R.string.reco_line_format, "Potassium", "$k mg/L", if(k<15) "Low" else "Good"))
        details.add(getString(R.string.reco_line_format, "Iron", "$fe mg/L", if(fe<0.1) "Low" else "Good"))
        
        val tempMessage = when {
            temp < 22 -> R.string.reco_msg_temp_low
            temp > 30 -> R.string.reco_msg_temp_high
            else -> R.string.reco_msg_temp_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_temp,
                formatValueWithUnit(temp, 1, getString(R.string.unit_temp_c)),
                tempMessage
            )
        )

        setRecommendationsText(buildRecommendationsText(actions, details))

        ghBinding?.let { updateHardnessStatus(it, viewModel.getGhInDegrees()) }
        khBinding?.let { updateHardnessStatus(it, viewModel.getKhInDegrees()) }
    }

    private fun updateSaltwaterRecommendations() {
        val litres = viewModel.getEffectiveVolumeLitres()
        val salinity = viewModel.salinity.value ?: 0.0
        val alkalinity = viewModel.alkalinity.value ?: 0.0
        val calcium = viewModel.calcium.value ?: 0.0
        val magnesium = viewModel.magnesium.value ?: 0.0
        val nitrate = viewModel.nitrate.value ?: 0.0
        val phosphate = viewModel.phosphate.value ?: 0.0
        val ph = viewModel.ph.value ?: 0.0
        val temp = viewModel.temperature.value ?: 0.0
        val sr = viewModel.strontium.value ?: 0.0
        val i = viewModel.iodide.value ?: 0.0

        val actions = mutableListOf<String>()
        if (litres <= 0) {
            actions.add(getString(R.string.reco_action_volume_needed))
        }
        if (nitrate > 20) {
            if (litres > 0) {
                val changeLitres = formatValue(viewModel.calculateWaterChangeLitres(20.0), 1)
                actions.add(getString(R.string.reco_action_nitrate_reef_high, changeLitres))
            } else {
                actions.add(getString(R.string.reco_action_nitrate_reef_high_generic))
            }
        }
        if (phosphate > 0.1) {
            if (litres > 0) {
                val changeLitres = formatValue(viewModel.calculateWaterChangeLitres(15.0), 1)
                actions.add(getString(R.string.reco_action_phosphate_high, changeLitres))
            } else {
                actions.add(getString(R.string.reco_action_phosphate_high_generic))
            }
        }
        if (sr < 8) actions.add("Low Strontium: Dose Reef Strontium.")
        if (i < 0.06) actions.add("Low Iodide: Dose Reef Iodide.")

        val details = mutableListOf<String>()
        val salinityMessage = when {
            salinity > 36 -> R.string.reco_msg_salinity_high
            salinity < 33 -> R.string.reco_msg_salinity_low
            else -> R.string.reco_msg_salinity_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_salinity,
                formatValueWithUnit(salinity, 1, getString(R.string.unit_salinity_ppt)),
                salinityMessage
            )
        )
        val alkMessage = when {
            alkalinity > 11 -> R.string.reco_msg_alkalinity_high
            alkalinity < 7 -> R.string.reco_msg_alkalinity_low
            else -> R.string.reco_msg_alkalinity_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_alkalinity,
                formatValueWithUnit(alkalinity, 1, getString(R.string.unit_dkh_short)),
                alkMessage
            )
        )
        val calciumMessage = when {
            calcium > 450 -> R.string.reco_msg_calcium_high
            calcium < 380 -> R.string.reco_msg_calcium_low
            else -> R.string.reco_msg_calcium_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_calcium,
                formatValueWithUnit(calcium, 0, getString(R.string.unit_calcium_ppm)),
                calciumMessage
            )
        )
        val magnesiumMessage = when {
            magnesium > 1450 -> R.string.reco_msg_magnesium_high
            magnesium < 1200 -> R.string.reco_msg_magnesium_low
            else -> R.string.reco_msg_magnesium_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_magnesium,
                formatValueWithUnit(magnesium, 0, getString(R.string.unit_magnesium_ppm)),
                magnesiumMessage
            )
        )
        val nitrateMessage = when {
            nitrate > 20 -> R.string.reco_msg_nitrate_reef_high
            nitrate > 5 -> R.string.reco_msg_nitrate_reef_moderate
            else -> R.string.reco_msg_nitrate_reef_low
        }
        details.add(
            buildRecommendationLine(
                R.string.param_nitrate,
                formatValueWithUnit(nitrate, 1, getString(R.string.unit_ppm)),
                nitrateMessage
            )
        )
        val phosphateMessage = when {
            phosphate > 0.1 -> R.string.reco_msg_phosphate_high
            phosphate < 0.03 -> R.string.reco_msg_phosphate_low
            else -> R.string.reco_msg_phosphate_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_phosphate,
                formatValueWithUnit(phosphate, 3, getString(R.string.unit_phosphate_ppm)),
                phosphateMessage
            )
        )
        val phMessage = when {
            ph > 8.5 -> R.string.reco_msg_ph_salt_high
            ph < 7.8 -> R.string.reco_msg_ph_salt_low
            else -> R.string.reco_msg_ph_salt_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_ph,
                formatValueWithUnit(ph, 2, getString(R.string.unit_ph)),
                phMessage
            )
        )
        details.add(getString(R.string.reco_line_format, "Strontium", "$sr mg/L", if(sr<8) "Low" else "Good"))
        details.add(getString(R.string.reco_line_format, "Iodide", "$i mg/L", if(i<0.06) "Low" else "Good"))
        
        val tempMessage = when {
            temp > 27 -> R.string.reco_msg_temp_salt_high
            temp < 24 -> R.string.reco_msg_temp_salt_low
            else -> R.string.reco_msg_temp_salt_ok
        }
        details.add(
            buildRecommendationLine(
                R.string.param_temp,
                formatValueWithUnit(temp, 1, getString(R.string.unit_temp_c)),
                tempMessage
            )
        )

        setRecommendationsText(buildRecommendationsText(actions, details))
    }

    private fun updatePondRecommendations() {
        // Minimal recommendations for utility profile
        val litres = viewModel.getEffectiveVolumeLitres()
        val actions = mutableListOf<String>()
        
        if (litres <= 0) {
            actions.add(getString(R.string.reco_action_volume_needed))
        }
        // No parameters to check
        
        val details = listOf("Misc Utility Profile: No parameters tracked.")
        setRecommendationsText(buildRecommendationsText(actions, details))
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
        if (_binding == null) return
        when (viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER) {
            MainViewModel.AquariumProfile.FRESHWATER -> updateFreshwaterRecommendations()
            MainViewModel.AquariumProfile.SALTWATER -> updateSaltwaterRecommendations()
            MainViewModel.AquariumProfile.POND -> updatePondRecommendations()
        }
    }

    private fun observeViewModel() {
        viewModel.ghUnit.observe(viewLifecycleOwner) { unit ->
            applyHardnessToggleState(ghBinding, unit)
            syncGhInput(unit)
            scheduleRecommendationsUpdate()
        }
        viewModel.khUnit.observe(viewLifecycleOwner) { unit ->
            applyHardnessToggleState(khBinding, unit)
            syncKhInput(unit)
            scheduleRecommendationsUpdate()
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
            setupParameters(it)
            applyProfileUi(it)
            scheduleRecommendationsUpdate()
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

    private fun updateActiveHardnessBindings(profile: MainViewModel.AquariumProfile) {
        ghBinding = when (profile) {
            MainViewModel.AquariumProfile.FRESHWATER -> ghFreshwaterBinding
            else -> null
        }
        khBinding = when (profile) {
            MainViewModel.AquariumProfile.FRESHWATER -> khFreshwaterBinding
            else -> null
        }
    }

    private fun syncGhInput(unit: String) {
        ghBinding?.etValue?.let { editText ->
            if (!editText.isFocused) {
                isGhUpdating = true
                editText.setText(formatHardnessInput(viewModel.gh.value ?: 0.0, unit))
                isGhUpdating = false
            }
        }
    }

    private fun syncKhInput(unit: String) {
        khBinding?.etValue?.let { editText ->
            if (!editText.isFocused) {
                isKhUpdating = true
                editText.setText(formatHardnessInput(viewModel.kh.value ?: 0.0, unit))
                isKhUpdating = false
            }
        }
    }

    private fun applyHardnessToggleState(binding: ItemParameterHardnessBinding?, unit: String) {
        binding?.toggleUnit?.check(if (unit == "ppm") R.id.btnPpm else R.id.btnDh)
    }

    private fun scheduleRecommendationsUpdate() {
        recommendationsHandler.removeCallbacks(recommendationsRunnable)
        recommendationsHandler.postDelayed(recommendationsRunnable, recommendationsUpdateDelayMs)
    }

    private fun setRecommendationsText(text: String) {
        if (binding.tvRecommendations.text?.toString() != text) {
            binding.tvRecommendations.text = text
        }
    }

    private fun buildRecommendationLine(labelRes: Int, value: String, messageRes: Int): String {
        return getString(R.string.reco_line_format, getString(labelRes), value, getString(messageRes))
    }

    private fun formatValue(value: Double, decimals: Int): String {
        return String.format("%.${decimals}f", value)
    }

    private fun formatValueWithUnit(value: Double, decimals: Int, unit: String): String {
        return "${formatValue(value, decimals)} $unit"
    }

    private fun appendSection(builder: StringBuilder, title: String, lines: List<String>) {
        if (lines.isEmpty()) return
        if (builder.isNotEmpty()) {
            builder.append("\n\n")
        }
        builder.append(title)
        lines.forEach { line ->
            builder.append("\n- ").append(line)
        }
    }

    private fun buildRecommendationsText(actions: List<String>, details: List<String>): String {
        val builder = StringBuilder()
        appendSection(builder, getString(R.string.reco_section_actions), actions)
        appendSection(builder, getString(R.string.reco_section_interpretation), details)
        return builder.toString().trim()
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
        val profile = viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER
        setupParameters(profile)
        applyProfileUi(profile)
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
        updateRecommendations()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recommendationsHandler.removeCallbacks(recommendationsRunnable)
        _binding = null
        ghBinding = null
        khBinding = null
        ghFreshwaterBinding = null
        khFreshwaterBinding = null
        // Removed khPondBinding
        configuredProfiles.clear()
    }
}
