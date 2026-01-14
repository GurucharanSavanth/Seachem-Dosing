package com.example.seachem_dosing.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.BuildConfig
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel

class SettingsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    private var currentThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    private var currentVolumeUnit = "US"
    private var currentHardnessUnit = "dh"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSetting(view)
        setupVolumeUnitSetting(view)
        setupHardnessUnitSetting(view)
        setupResetSetting(view)
        setupVersion(view)
    }

    private fun setupThemeSetting(view: View) {
        val settingTheme = view.findViewById<LinearLayout>(R.id.settingTheme)
        val tvCurrentTheme = view.findViewById<TextView>(R.id.tvCurrentTheme)

        updateThemeText(tvCurrentTheme)

        settingTheme.setOnClickListener {
            showThemeDialog(tvCurrentTheme)
        }
    }

    private fun showThemeDialog(tvCurrentTheme: TextView) {
        val themes = arrayOf(
            getString(R.string.settings_theme_system),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark)
        )

        val currentSelection = when (currentThemeMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> 0
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_theme))
            .setSingleChoiceItems(themes, currentSelection) { dialog, which ->
                currentThemeMode = when (which) {
                    0 -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(currentThemeMode)
                updateThemeText(tvCurrentTheme)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateThemeText(tvCurrentTheme: TextView) {
        tvCurrentTheme.text = when (currentThemeMode) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.settings_theme_system)
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_theme_dark)
            else -> getString(R.string.settings_theme_system)
        }
    }

    private fun setupVolumeUnitSetting(view: View) {
        val settingVolumeUnit = view.findViewById<LinearLayout>(R.id.settingVolumeUnit)
        val tvCurrentVolumeUnit = view.findViewById<TextView>(R.id.tvCurrentVolumeUnit)

        currentVolumeUnit = viewModel.volumeUnit.value ?: "US"
        updateVolumeUnitText(tvCurrentVolumeUnit)

        settingVolumeUnit.setOnClickListener {
            showVolumeUnitDialog(tvCurrentVolumeUnit)
        }
    }

    private fun showVolumeUnitDialog(tvCurrentVolumeUnit: TextView) {
        val units = arrayOf(
            getString(R.string.unit_us_gallon),
            getString(R.string.unit_litre),
            getString(R.string.unit_uk_gallon)
        )

        val currentSelection = when (currentVolumeUnit) {
            "US" -> 0
            "L" -> 1
            "UK" -> 2
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_volume_unit))
            .setSingleChoiceItems(units, currentSelection) { dialog, which ->
                currentVolumeUnit = when (which) {
                    0 -> "US"
                    1 -> "L"
                    2 -> "UK"
                    else -> "US"
                }
                viewModel.setVolumeUnit(currentVolumeUnit)
                updateVolumeUnitText(tvCurrentVolumeUnit)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateVolumeUnitText(tvCurrentVolumeUnit: TextView) {
        tvCurrentVolumeUnit.text = when (currentVolumeUnit) {
            "US" -> getString(R.string.unit_us_gallon)
            "L" -> getString(R.string.unit_litre)
            "UK" -> getString(R.string.unit_uk_gallon)
            else -> getString(R.string.unit_us_gallon)
        }
    }

    private fun setupHardnessUnitSetting(view: View) {
        val settingHardnessUnit = view.findViewById<LinearLayout>(R.id.settingHardnessUnit)
        val tvCurrentHardnessUnit = view.findViewById<TextView>(R.id.tvCurrentHardnessUnit)

        currentHardnessUnit = viewModel.ghUnit.value ?: "dh"
        updateHardnessUnitText(tvCurrentHardnessUnit)

        settingHardnessUnit.setOnClickListener {
            showHardnessUnitDialog(tvCurrentHardnessUnit)
        }
    }

    private fun showHardnessUnitDialog(tvCurrentHardnessUnit: TextView) {
        val units = arrayOf(
            getString(R.string.unit_dgh),
            getString(R.string.unit_ppm)
        )

        val currentSelection = if (currentHardnessUnit == "dh") 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_hardness_unit))
            .setSingleChoiceItems(units, currentSelection) { dialog, which ->
                currentHardnessUnit = if (which == 0) "dh" else "ppm"
                viewModel.setGhUnit(currentHardnessUnit)
                viewModel.setKhUnit(currentHardnessUnit)
                updateHardnessUnitText(tvCurrentHardnessUnit)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateHardnessUnitText(tvCurrentHardnessUnit: TextView) {
        tvCurrentHardnessUnit.text = if (currentHardnessUnit == "dh") {
            getString(R.string.unit_dgh)
        } else {
            getString(R.string.unit_ppm)
        }
    }

    private fun setupResetSetting(view: View) {
        val settingReset = view.findViewById<LinearLayout>(R.id.settingReset)

        settingReset.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_reset))
            .setMessage(getString(R.string.settings_reset_confirm))
            .setPositiveButton(getString(R.string.settings_reset)) { _, _ ->
                viewModel.resetAll()
                // Reset theme to system default
                currentThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(currentThemeMode)
                // Refresh UI
                view?.let { setupThemeSetting(it) }
                view?.let { setupVolumeUnitSetting(it) }
                view?.let { setupHardnessUnitSetting(it) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupVersion(view: View) {
        val tvVersion = view.findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }
}
