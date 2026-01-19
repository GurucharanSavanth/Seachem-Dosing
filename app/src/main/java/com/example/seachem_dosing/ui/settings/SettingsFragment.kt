package com.example.seachem_dosing.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.BuildConfig
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel
import com.google.android.material.transition.MaterialFadeThrough

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSetting(view)
        setupLanguageSetting(view)
        setupVolumeUnitSetting(view)
        setupHardnessUnitSetting(view)
        setupWaterChangeSetting(view)
        setupExportSetting(view)
        setupResetSetting(view)
        setupSupportSettings(view)
        setupVersion(view)
    }

    private fun setupThemeSetting(view: View) {
        val settingTheme = view.findViewById<LinearLayout>(R.id.settingTheme)
        val tvCurrentTheme = view.findViewById<TextView>(R.id.tvCurrentTheme)

        currentThemeMode = AppCompatDelegate.getDefaultNightMode().let { mode ->
            if (mode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            } else {
                mode
            }
        }
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

    private fun setupLanguageSetting(view: View) {
        val settingLanguage = view.findViewById<LinearLayout>(R.id.settingLanguage)
        val tvCurrentLanguage = view.findViewById<TextView>(R.id.tvCurrentLanguage)

        // Detect current app locale
        val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
        val langCode = currentLocale?.language ?: "en"
        
        tvCurrentLanguage.text = if (langCode == "kn") getString(R.string.settings_language_kn) else getString(R.string.settings_language_en)

        settingLanguage.setOnClickListener {
            showLanguageDialog(tvCurrentLanguage)
        }
    }

    private fun showLanguageDialog(tvCurrentLanguage: TextView) {
        val languages = arrayOf(
            getString(R.string.settings_language_en),
            getString(R.string.settings_language_kn)
        )
        
        // Check current
        val currentLocale = AppCompatDelegate.getApplicationLocales().get(0)
        val currentCode = currentLocale?.language ?: "en"
        val checkedItem = if (currentCode == "kn") 1 else 0

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_language))
            .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                val newLocale = if (which == 1) "kn" else "en"
                
                // Apply Locale
                val appLocale = LocaleListCompat.forLanguageTags(newLocale)
                AppCompatDelegate.setApplicationLocales(appLocale)
                
                dialog.dismiss()
                // Activity will likely recreate, but if not:
                tvCurrentLanguage.text = languages[which]
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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
                val oldUnit = currentHardnessUnit
                currentHardnessUnit = if (which == 0) "dh" else "ppm"
                if (currentHardnessUnit != oldUnit) {
                    viewModel.updateHardnessUnit(currentHardnessUnit)
                }
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

    private fun setupWaterChangeSetting(view: View) {
        val settingWaterChange = view.findViewById<LinearLayout>(R.id.settingWaterChange)
        val tvCurrentWaterChange = view.findViewById<TextView>(R.id.tvCurrentWaterChange)

        viewModel.defaultWaterChangePercent.observe(viewLifecycleOwner) { percent ->
            tvCurrentWaterChange.text = "${percent.toInt()}%"
        }

        settingWaterChange.setOnClickListener {
            showWaterChangeDialog()
        }
    }

    private fun showWaterChangeDialog() {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.setText(viewModel.defaultWaterChangePercent.value.toString())

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_water_change_dialog_title))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull()
                if (newValue != null && newValue in 0.0..100.0) {
                    viewModel.setDefaultWaterChangePercent(newValue)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupExportSetting(view: View) {
        val settingExport = view.findViewById<LinearLayout>(R.id.settingExport)
        settingExport.setOnClickListener {
            val data = viewModel.generateExportData()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Seachem Dosing Data", data)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT).show()
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
    
    private fun setupSupportSettings(view: View) {
        view.findViewById<LinearLayout>(R.id.settingContact).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("savanthgc@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Seachem Dosing App Support")
            }
            startActivity(Intent.createChooser(intent, getString(R.string.settings_contact)))
        }

        view.findViewById<LinearLayout>(R.id.settingSourceCode).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.settings_source_url)))
            startActivity(intent)
        }
    }

    private fun setupVersion(view: View) {
        val tvVersion = view.findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = getString(R.string.settings_version, BuildConfig.VERSION_NAME)
    }
}
