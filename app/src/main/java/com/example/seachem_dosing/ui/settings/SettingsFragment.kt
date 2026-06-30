package com.example.seachem_dosing.ui.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.seachem_dosing.BuildConfig
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.theme.SeachemTheme
import com.google.android.material.transition.MaterialFadeThrough

/**
 * Hosts the Compose [SettingsScreen] (ADR-001 migration). Theme/locale via
 * AppCompatDelegate, unit/water-change via the ViewModel, dialogs via
 * AlertDialog — all unchanged from the XML version. Local Compose state mirrors
 * the three ViewModel-backed labels (theme/locale changes recreate the Activity).
 */
class SettingsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var currentThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

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
                currentThemeMode = resolveThemeMode()
                var themeMode by remember { mutableIntStateOf(currentThemeMode) }
                var volumeUnit by remember { mutableStateOf(viewModel.volumeUnit.value ?: "US") }
                var ghUnit by remember { mutableStateOf(viewModel.ghUnit.value ?: "dh") }
                var waterChange by remember { mutableDoubleStateOf(viewModel.defaultWaterChangePercent.value ?: 20.0) }

                SettingsScreen(
                    themeLabel = themeLabel(themeMode),
                    languageLabel = languageLabel(),
                    volumeUnitLabel = volumeUnitLabel(volumeUnit),
                    hardnessUnitLabel = hardnessUnitLabel(ghUnit),
                    waterChangeLabel = "${waterChange.toInt()}%",
                    versionText = getString(R.string.settings_version, BuildConfig.VERSION_NAME),
                    onThemeClick = { showThemeDialog { mode -> themeMode = mode } },
                    onLanguageClick = { showLanguageDialog() },
                    onVolumeUnitClick = { showVolumeUnitDialog { unit -> volumeUnit = unit } },
                    onHardnessClick = { showHardnessUnitDialog { unit -> ghUnit = unit } },
                    onWaterChangeClick = { showWaterChangeDialog { pct -> waterChange = pct } },
                    onExportClick = { exportData() },
                    onResetClick = { showResetConfirmationDialog { themeMode = currentThemeMode } },
                    onContactClick = { contactSupport() },
                    onSourceClick = { openSourceCode() },
                )
            }
        }
    }

    // --- labels ---

    private fun resolveThemeMode(): Int = AppCompatDelegate.getDefaultNightMode().let { mode ->
        if (mode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM else mode
    }

    private fun themeLabel(mode: Int): String = when (mode) {
        AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.settings_theme_light)
        AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.settings_theme_dark)
        else -> getString(R.string.settings_theme_system)
    }

    private fun languageLabel(): String {
        val langCode = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
        return if (langCode == "kn") getString(R.string.settings_language_kn) else getString(R.string.settings_language_en)
    }

    private fun volumeUnitLabel(unit: String): String = when (unit) {
        "L" -> getString(R.string.unit_litre)
        "UK" -> getString(R.string.unit_uk_gallon)
        else -> getString(R.string.unit_us_gallon)
    }

    private fun hardnessUnitLabel(unit: String): String =
        if (unit == "dh") getString(R.string.unit_dgh) else getString(R.string.unit_ppm)

    // --- dialogs (logic identical to XML version; result returned via callback) ---

    private fun showThemeDialog(onPicked: (Int) -> Unit) {
        val themes = arrayOf(
            getString(R.string.settings_theme_system),
            getString(R.string.settings_theme_light),
            getString(R.string.settings_theme_dark),
        )
        val current = when (currentThemeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_theme))
            .setSingleChoiceItems(themes, current) { dialog, which ->
                currentThemeMode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(currentThemeMode)
                onPicked(currentThemeMode)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.settings_language_en), getString(R.string.settings_language_kn))
        val currentCode = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
        val checked = if (currentCode == "kn") 1 else 0
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_language))
            .setSingleChoiceItems(languages, checked) { dialog, which ->
                val locale = if (which == 1) "kn" else "en"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale))
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showVolumeUnitDialog(onPicked: (String) -> Unit) {
        val units = arrayOf(
            getString(R.string.unit_us_gallon),
            getString(R.string.unit_litre),
            getString(R.string.unit_uk_gallon),
        )
        val current = when (viewModel.volumeUnit.value ?: "US") { "L" -> 1; "UK" -> 2; else -> 0 }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_volume_unit))
            .setSingleChoiceItems(units, current) { dialog, which ->
                val unit = when (which) { 1 -> "L"; 2 -> "UK"; else -> "US" }
                viewModel.setVolumeUnit(unit)
                onPicked(unit)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showHardnessUnitDialog(onPicked: (String) -> Unit) {
        val units = arrayOf(getString(R.string.unit_dgh), getString(R.string.unit_ppm))
        val current = if ((viewModel.ghUnit.value ?: "dh") == "dh") 0 else 1
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_hardness_unit))
            .setSingleChoiceItems(units, current) { dialog, which ->
                val unit = if (which == 0) "dh" else "ppm"
                viewModel.updateHardnessUnit(unit)
                onPicked(unit)
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showWaterChangeDialog(onPicked: (Double) -> Unit) {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(viewModel.defaultWaterChangePercent.value.toString())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_water_change_dialog_title))
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val value = input.text.toString().toDoubleOrNull()
                if (value != null && value in 0.0..100.0) {
                    viewModel.setDefaultWaterChangePercent(value)
                    onPicked(value)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showResetConfirmationDialog(onReset: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_reset))
            .setMessage(getString(R.string.settings_reset_confirm))
            .setPositiveButton(getString(R.string.settings_reset)) { _, _ ->
                viewModel.resetAll()
                currentThemeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(currentThemeMode)
                onReset()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // --- actions ---

    private fun exportData() {
        val data = viewModel.generateExportData()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Seachem Dosing Data", data))
        Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT).show()
    }

    private fun contactSupport() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf("savanthgc@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Seachem Dosing App Support")
        }
        startActivitySafely(Intent.createChooser(intent, getString(R.string.settings_contact)))
    }

    private fun openSourceCode() {
        startActivitySafely(Intent(Intent.ACTION_VIEW, getString(R.string.settings_source_url).toUri()))
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.settings_no_app_available, Toast.LENGTH_SHORT).show()
        }
    }
}
