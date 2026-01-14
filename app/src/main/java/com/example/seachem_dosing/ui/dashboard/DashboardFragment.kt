package com.example.seachem_dosing.ui.dashboard

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.R
import com.example.seachem_dosing.databinding.FragmentDashboardBinding
import com.example.seachem_dosing.databinding.ItemParameterBinding
import com.example.seachem_dosing.databinding.ItemParameterHardnessBinding
import com.example.seachem_dosing.ui.MainViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupVolumeSection()
        setupParameters()
        observeViewModel()
        updateRecommendations()
    }

    private fun setupVolumeSection() {
        // Volume mode toggle
        binding.toggleVolumeMode.check(R.id.btnDirect)
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
                updateRecommendations()
            }
        }

        // Volume unit dropdown
        val volumeUnits = arrayOf("US Gallons", "Litres", "UK Gallons")
        val volumeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, volumeUnits)
        binding.spinnerVolumeUnit.setAdapter(volumeAdapter)
        binding.spinnerVolumeUnit.setText(volumeUnits[0], false)
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
        val dimUnits = arrayOf("cm", "in", "ft")
        val dimAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, dimUnits)
        binding.spinnerDimUnit.setAdapter(dimAdapter)
        binding.spinnerDimUnit.setText(dimUnits[0], false)
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
        val ghBinding = ItemParameterHardnessBinding.bind(binding.paramGh.root)
        ghBinding.tvParamName.text = getString(R.string.param_gh)
        ghBinding.etValue.setText(viewModel.gh.value?.toString() ?: "4")
        ghBinding.toggleUnit.check(R.id.btnDh)
        ghBinding.toggleUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val unit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                viewModel.setGhUnit(unit)
                viewModel.syncGhFromParams()
            }
        }
        ghBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setGh(value)
            viewModel.syncGhFromParams()
            updateRecommendations()
        })

        // KH
        val khBinding = ItemParameterHardnessBinding.bind(binding.paramKh.root)
        khBinding.tvParamName.text = getString(R.string.param_kh)
        khBinding.etValue.setText(viewModel.kh.value?.toString() ?: "6")
        khBinding.toggleUnit.check(R.id.btnDh)
        khBinding.toggleUnit.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val unit = if (checkedId == R.id.btnDh) "dh" else "ppm"
                viewModel.setKhUnit(unit)
                viewModel.syncKhFromParams()
            }
        }
        khBinding.etValue.addTextChangedListener(createTextWatcher { value ->
            viewModel.setKh(value)
            viewModel.syncKhFromParams()
            updateRecommendations()
        })
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

    private fun updateRecommendations() {
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
    }

    private fun observeViewModel() {
        // Observe LiveData if needed for reactive updates
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
