package com.example.seachem_dosing.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.R
import com.example.seachem_dosing.domain.engine.RecommendationEngine
import com.example.seachem_dosing.logic.Calculations
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.MainViewModel.AquariumProfile
import com.example.seachem_dosing.ui.MainViewModel.Status

private val StatusGood = Color(0xFF4CAF50)
private val StatusWarning = Color(0xFFFF9800)
private val StatusDanger = Color(0xFFF44336)
private val StatusInfo = Color(0xFF2196F3)

private fun Status.color(): Color = when (this) {
    Status.GOOD -> StatusGood
    Status.WARNING -> StatusWarning
    Status.DANGER -> StatusDanger
    Status.INFO -> StatusInfo
}

/** Hardness status mirrors DashboardFragment.updateHardnessStatus: 0.1–2.99 dH = warning. */
private fun hardnessColor(degrees: Double): Color =
    if (degrees in 0.1..2.99) StatusWarning else StatusInfo

/**
 * Compose port of fragment_dashboard.xml (ADR-001). Volume + parameter inputs
 * drive the ViewModel; recommendations are computed by the (already-extracted)
 * [RecommendationEngine] and rendered via stringResource. Copy/share stay in
 * [DashboardFragment] via callbacks.
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.profile.observeAsState(AquariumProfile.FRESHWATER)

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.dashboard_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))
        VolumeSection(viewModel)

        if (profile != AquariumProfile.POND) {
            SectionHeader(stringResource(R.string.section_parameters))
            when (profile) {
                AquariumProfile.FRESHWATER -> FreshwaterParams(viewModel)
                AquariumProfile.SALTWATER -> SaltwaterParams(viewModel)
                else -> Unit
            }
        }

        SectionHeader(stringResource(R.string.section_recommendations))
        RecommendationsCard(viewModel, profile, onCopy, onShare)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
}

// ---------------- Volume ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSection(viewModel: MainViewModel) {
    val mode by viewModel.volumeMode.observeAsState("direct")
    val volume by viewModel.volume.observeAsState(10.0)
    val volumeUnit by viewModel.volumeUnit.observeAsState("US")
    val length by viewModel.dimLength.observeAsState(60.0)
    val breadth by viewModel.dimBreadth.observeAsState(30.0)
    val heightV by viewModel.dimHeight.observeAsState(40.0)
    val dimUnit by viewModel.dimUnit.observeAsState("cm")

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode != "lbh",
                    onClick = { viewModel.setVolumeMode("direct") },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(R.string.volume_direct)) }
                SegmentedButton(
                    selected = mode == "lbh",
                    onClick = { viewModel.setVolumeMode("lbh") },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(R.string.volume_lbh)) }
            }
            Spacer(Modifier.height(12.dp))

            if (mode == "lbh") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("L", length, Modifier.weight(1f)) { viewModel.setDimLength(it) }
                    NumberField("B", breadth, Modifier.weight(1f)) { viewModel.setDimBreadth(it) }
                    NumberField("H", heightV, Modifier.weight(1f)) { viewModel.setDimHeight(it) }
                }
                Spacer(Modifier.height(8.dp))
                UnitDropdown(
                    options = listOf(stringResource(R.string.unit_cm), stringResource(R.string.unit_in), stringResource(R.string.unit_ft)),
                    selectedIndex = when (dimUnit) { "in" -> 1; "ft" -> 2; else -> 0 },
                ) { viewModel.setDimUnit(listOf("cm", "in", "ft")[it]) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(stringResource(R.string.nav_dashboard).let { "" }, volume, Modifier.weight(1f)) { viewModel.setVolume(it) }
                    Box(Modifier.weight(1f)) {
                        UnitDropdown(
                            options = listOf(stringResource(R.string.unit_us_gallon), stringResource(R.string.unit_litre), stringResource(R.string.unit_uk_gallon)),
                            selectedIndex = when (volumeUnit) { "L" -> 1; "UK" -> 2; else -> 0 },
                        ) { viewModel.setVolumeUnit(listOf("US", "L", "UK")[it]) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.calculated_volume, String.format("%.1f", viewModel.getEffectiveVolumeLitres())),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { options.firstOrNull().orEmpty() },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, label ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(i); expanded = false })
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: Double, modifier: Modifier = Modifier, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toDoubleOrNull()?.let(onChange) },
        label = if (label.isNotEmpty()) ({ Text(label) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

// ---------------- Parameters ----------------

@Composable
private fun ParamRow(label: String, value: Double, unit: String, statusColor: Color, onChange: (Double) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 6.dp, height = 36.dp).background(statusColor, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            NumberField("", value, Modifier.width(110.dp)) { onChange(it) }
            Spacer(Modifier.width(8.dp))
            Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HardnessRow(label: String, value: Double, unit: String, degrees: Double, onValue: (Double) -> Unit, onUnit: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 6.dp, height = 36.dp).background(hardnessColor(degrees), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                NumberField("", value, Modifier.width(96.dp)) { onValue(it) }
            }
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = unit == "dh", onClick = { onUnit("dh") }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.unit_dgh_short)) }
                SegmentedButton(selected = unit == "ppm", onClick = { onUnit("ppm") }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.unit_ppm_caps)) }
            }
        }
    }
}

@Composable
private fun FreshwaterParams(viewModel: MainViewModel) {
    val ammonia by viewModel.ammonia.observeAsState(0.0)
    val nitrite by viewModel.nitrite.observeAsState(0.0)
    val nitrate by viewModel.nitrate.observeAsState(15.0)
    val gh by viewModel.gh.observeAsState(4.0)
    val kh by viewModel.kh.observeAsState(6.0)
    val ghUnit by viewModel.ghUnit.observeAsState("dh")
    val khUnit by viewModel.khUnit.observeAsState("dh")
    val ph by viewModel.ph.observeAsState(7.2)
    val potassium by viewModel.potassium.observeAsState(15.0)
    val iron by viewModel.iron.observeAsState(0.1)
    val temp by viewModel.temperature.observeAsState(26.0)

    ParamRow(stringResource(R.string.param_ammonia), ammonia, stringResource(R.string.unit_ppm), viewModel.getAmmoniaStatus().status.color()) { viewModel.setAmmonia(it) }
    ParamRow(stringResource(R.string.param_nitrite), nitrite, stringResource(R.string.unit_ppm), viewModel.getNitriteStatus().status.color()) { viewModel.setNitrite(it) }
    ParamRow(stringResource(R.string.param_nitrate), nitrate, stringResource(R.string.unit_ppm), viewModel.getNitrateStatus().status.color()) { viewModel.setNitrate(it) }
    HardnessRow(stringResource(R.string.param_gh), gh, ghUnit, viewModel.getGhInDegrees(), { viewModel.setGh(it) }, { convertHardness(viewModel, gh, ghUnit, it, isGh = true) })
    HardnessRow(stringResource(R.string.param_kh), kh, khUnit, viewModel.getKhInDegrees(), { viewModel.setKh(it) }, { convertHardness(viewModel, kh, khUnit, it, isGh = false) })
    ParamRow(stringResource(R.string.param_ph), ph, stringResource(R.string.unit_ph), StatusInfo) { viewModel.setPh(it) }
    ParamRow(stringResource(R.string.param_potassium), potassium, stringResource(R.string.unit_mg_l), StatusInfo) { viewModel.setPotassium(it) }
    ParamRow(stringResource(R.string.param_iron), iron, stringResource(R.string.unit_mg_l), StatusInfo) { viewModel.setIron(it) }
    ParamRow(stringResource(R.string.param_temp), temp, stringResource(R.string.unit_temp_c), StatusInfo) { viewModel.setTemperature(it) }
}

/** GH/KH unit toggle: convert the stored value old→new unit, then persist value + unit. */
private fun convertHardness(vm: MainViewModel, current: Double, oldUnit: String, newUnit: String, isGh: Boolean) {
    if (oldUnit == newUnit) return
    val converted = when {
        oldUnit == "ppm" && newUnit == "dh" -> Calculations.ppmToDh(current)
        oldUnit == "dh" && newUnit == "ppm" -> Calculations.dhToPpm(current)
        else -> current
    }
    if (isGh) { vm.setGh(converted); vm.setGhUnit(newUnit) } else { vm.setKh(converted); vm.setKhUnit(newUnit) }
}

@Composable
private fun SaltwaterParams(viewModel: MainViewModel) {
    val salinity by viewModel.salinity.observeAsState(35.0)
    val alkalinity by viewModel.alkalinity.observeAsState(8.0)
    val calcium by viewModel.calcium.observeAsState(420.0)
    val magnesium by viewModel.magnesium.observeAsState(1300.0)
    val nitrate by viewModel.nitrate.observeAsState(10.0)
    val phosphate by viewModel.phosphate.observeAsState(0.05)
    val ph by viewModel.ph.observeAsState(8.2)
    val strontium by viewModel.strontium.observeAsState(8.0)
    val iodide by viewModel.iodide.observeAsState(0.06)
    val temp by viewModel.temperature.observeAsState(26.0)

    ParamRow(stringResource(R.string.param_salinity), salinity, stringResource(R.string.unit_salinity_ppt), StatusInfo) { viewModel.setSalinity(it) }
    ParamRow(stringResource(R.string.param_alkalinity), alkalinity, stringResource(R.string.unit_dkh), StatusInfo) { viewModel.setAlkalinity(it) }
    ParamRow(stringResource(R.string.param_calcium), calcium, stringResource(R.string.unit_calcium_ppm), StatusInfo) { viewModel.setCalcium(it) }
    ParamRow(stringResource(R.string.param_magnesium), magnesium, stringResource(R.string.unit_magnesium_ppm), StatusInfo) { viewModel.setMagnesium(it) }
    ParamRow(stringResource(R.string.param_nitrate), nitrate, stringResource(R.string.unit_ppm), StatusInfo) { viewModel.setNitrate(it) }
    ParamRow(stringResource(R.string.param_phosphate), phosphate, stringResource(R.string.unit_phosphate_ppm), StatusInfo) { viewModel.setPhosphate(it) }
    ParamRow(stringResource(R.string.param_ph), ph, stringResource(R.string.unit_ph), StatusInfo) { viewModel.setPh(it) }
    ParamRow(stringResource(R.string.param_strontium), strontium, stringResource(R.string.unit_mg_l), StatusInfo) { viewModel.setStrontium(it) }
    ParamRow(stringResource(R.string.param_iodide), iodide, stringResource(R.string.unit_mg_l), StatusInfo) { viewModel.setIodide(it) }
    ParamRow(stringResource(R.string.param_temp), temp, stringResource(R.string.unit_temp_c), StatusInfo) { viewModel.setTemperature(it) }
}

// ---------------- Recommendations ----------------

@Composable
private fun RecommendationsCard(viewModel: MainViewModel, profile: AquariumProfile, onCopy: (String) -> Unit, onShare: (String) -> Unit) {
    // Observe the inputs so the report recomputes when any parameter changes.
    val ammonia by viewModel.ammonia.observeAsState(0.0)
    val nitrite by viewModel.nitrite.observeAsState(0.0)
    val nitrate by viewModel.nitrate.observeAsState(0.0)
    val ph by viewModel.ph.observeAsState(0.0)
    val temp by viewModel.temperature.observeAsState(0.0)
    val potassium by viewModel.potassium.observeAsState(0.0)
    val iron by viewModel.iron.observeAsState(0.0)
    val salinity by viewModel.salinity.observeAsState(0.0)
    val alkalinity by viewModel.alkalinity.observeAsState(0.0)
    val calcium by viewModel.calcium.observeAsState(0.0)
    val magnesium by viewModel.magnesium.observeAsState(0.0)
    val phosphate by viewModel.phosphate.observeAsState(0.0)
    val strontium by viewModel.strontium.observeAsState(0.0)
    val iodide by viewModel.iodide.observeAsState(0.0)
    viewModel.gh.observeAsState(0.0).value
    viewModel.kh.observeAsState(0.0).value

    val litres = viewModel.getEffectiveVolumeLitres()
    val report = when (profile) {
        AquariumProfile.FRESHWATER -> RecommendationEngine.freshwater(
            RecommendationEngine.FreshwaterInput(litres, ammonia, nitrite, nitrate, viewModel.getGhInDegrees(), viewModel.getKhInDegrees(), ph, temp, potassium, iron)
        )
        AquariumProfile.SALTWATER -> RecommendationEngine.saltwater(
            RecommendationEngine.SaltwaterInput(litres, salinity, alkalinity, calcium, magnesium, nitrate, phosphate, ph, temp, strontium, iodide)
        )
        AquariumProfile.POND -> RecommendationEngine.pond(litres)
    }

    val actionLines = report.actions.map { if (it.arg != null) stringResource(it.res, it.arg!!) else stringResource(it.res) }
    val detailLines = report.details.map { line ->
        when (line) {
            is RecommendationEngine.Line.Param -> stringResource(R.string.reco_line_format, stringResource(line.labelRes), "${line.value} ${stringResource(line.unitRes)}", stringResource(line.msgRes))
            is RecommendationEngine.Line.Plain -> stringResource(line.res)
        }
    }
    val fullText = buildString {
        if (actionLines.isNotEmpty()) {
            append(stringResource(R.string.reco_section_actions))
            actionLines.forEach { append("\n- ").append(it) }
        }
        if (detailLines.isNotEmpty()) {
            if (isNotEmpty()) append("\n\n")
            append(stringResource(R.string.reco_section_interpretation))
            detailLines.forEach { append("\n- ").append(it) }
        }
    }.trim()

    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Text(fullText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(onClick = { onCopy(fullText) }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(stringResource(R.string.action_copy), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Surface(onClick = { onShare(fullText) }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(stringResource(R.string.action_share), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
