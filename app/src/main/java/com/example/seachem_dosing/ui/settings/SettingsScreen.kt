package com.example.seachem_dosing.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.R

/**
 * Compose port of fragment_settings.xml (ADR-001, parity-gated). Stateless:
 * all current values come in as labels; every tap is a callback handled by
 * [SettingsFragment] (dialogs + AppCompatDelegate + ViewModel stay there).
 */
@Composable
fun SettingsScreen(
    themeLabel: String,
    languageLabel: String,
    volumeUnitLabel: String,
    hardnessUnitLabel: String,
    waterChangeLabel: String,
    versionText: String,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onVolumeUnitClick: () -> Unit,
    onHardnessClick: () -> Unit,
    onWaterChangeClick: () -> Unit,
    onExportClick: () -> Unit,
    onResetClick: () -> Unit,
    onContactClick: () -> Unit,
    onSourceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.settings_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionHeader(R.string.settings_appearance)
        SettingRow(stringResource(R.string.settings_theme), themeLabel, onThemeClick)
        SettingRow(stringResource(R.string.settings_language), languageLabel, onLanguageClick)

        SectionHeader(R.string.settings_defaults)
        SettingRow(stringResource(R.string.settings_volume_unit), volumeUnitLabel, onVolumeUnitClick)
        SettingRow(stringResource(R.string.settings_hardness_unit), hardnessUnitLabel, onHardnessClick)
        SettingRow(stringResource(R.string.settings_water_change), waterChangeLabel, onWaterChangeClick)

        SectionHeader(R.string.settings_data)
        SettingRow(stringResource(R.string.settings_export), null, onExportClick)
        SettingRow(stringResource(R.string.settings_reset), null, onResetClick, danger = true)

        SectionHeader(R.string.settings_support)
        SettingRow(stringResource(R.string.settings_contact), null, onContactClick)
        SettingRow(stringResource(R.string.settings_source_code), null, onSourceClick)

        SectionHeader(R.string.settings_about)
        Card(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(versionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.settings_developer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.settings_source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_special_thanks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, value: String?, onClick: () -> Unit, danger: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (value != null) {
                Spacer(Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
