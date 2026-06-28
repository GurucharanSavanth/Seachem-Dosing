package com.example.seachem_dosing.ui.calculators

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.R
import com.example.seachem_dosing.logic.SeachemCalculations
import com.example.seachem_dosing.logic.SeachemCalculations.Product
import com.example.seachem_dosing.logic.SeachemCalculations.UnitScale
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.MainViewModel.AquariumProfile

private enum class Group { FW, SW }

private data class UniCard(
    val product: Product,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val showScale: Boolean,
    val showInputs: Boolean = true,
    val defaultScale: UnitScale = UnitScale.PPM,
    @StringRes val currentLabel: Int = R.string.label_current,
    @StringRes val targetLabel: Int = R.string.label_target,
    val group: Group,
)

// Faithful port of the setupUniversalCard(...) calls in CalculatorsFragment.
private val UNI_CARDS = listOf(
    UniCard(Product.FLOURISH, R.string.calc_flourish_title, R.string.calc_flourish_subtitle, showScale = false, showInputs = false, group = Group.FW),
    UniCard(Product.FLOURISH_TRACE, R.string.calc_flourish_trace_title, R.string.calc_flourish_trace_subtitle, showScale = false, showInputs = false, group = Group.FW),
    UniCard(Product.FLOURISH_IRON, R.string.calc_flourish_iron_title, R.string.calc_flourish_iron_subtitle, showScale = true, currentLabel = R.string.label_current_fe, targetLabel = R.string.label_target_fe, group = Group.FW),
    UniCard(Product.FLOURISH_NITROGEN, R.string.calc_flourish_nitrogen_title, R.string.calc_flourish_nitrogen_subtitle, showScale = true, currentLabel = R.string.label_current_n, targetLabel = R.string.label_target_n, group = Group.FW),
    UniCard(Product.FLOURISH_PHOSPHORUS, R.string.calc_flourish_phosphorus_title, R.string.calc_flourish_phosphorus_subtitle, showScale = true, currentLabel = R.string.label_current_p, targetLabel = R.string.label_target_p, group = Group.FW),
    UniCard(Product.FLOURISH_POTASSIUM, R.string.calc_flourish_potassium_title, R.string.calc_flourish_potassium_subtitle, showScale = true, currentLabel = R.string.label_current_k, targetLabel = R.string.label_target_k, group = Group.FW),
    UniCard(Product.ALKALINE_BUFFER, R.string.calc_alkaline_buffer_title, R.string.calc_alkaline_buffer_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_kh, targetLabel = R.string.label_target_kh, group = Group.FW),
    UniCard(Product.ACID_BUFFER, R.string.calc_acid_title, R.string.calc_acid_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_kh, targetLabel = R.string.label_target_kh, group = Group.FW),
    UniCard(Product.POTASSIUM_BICARBONATE, R.string.calc_khco3_title, R.string.calc_khco3_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_kh, targetLabel = R.string.label_target_kh, group = Group.FW),
    UniCard(Product.EQUILIBRIUM, R.string.calc_equilibrium_title, R.string.calc_equilibrium_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_gh, targetLabel = R.string.label_target_gh, group = Group.FW),
    UniCard(Product.NEUTRAL_REGULATOR, R.string.calc_neutral_title, R.string.calc_neutral_subtitle, showScale = false, currentLabel = R.string.label_current_ph, targetLabel = R.string.label_target_ph, group = Group.FW),
    UniCard(Product.REEF_ADVANTAGE_CALCIUM, R.string.calc_reef_adv_calcium_title, R.string.calc_reef_adv_calcium_subtitle, showScale = true, currentLabel = R.string.label_current_ca, targetLabel = R.string.label_target_ca, group = Group.SW),
    UniCard(Product.REEF_ADVANTAGE_MAGNESIUM, R.string.calc_reef_adv_magnesium_title, R.string.calc_reef_adv_magnesium_subtitle, showScale = true, currentLabel = R.string.label_current_mg, targetLabel = R.string.label_target_mg, group = Group.SW),
    UniCard(Product.REEF_ADVANTAGE_STRONTIUM, R.string.calc_reef_adv_strontium_title, R.string.calc_reef_adv_strontium_subtitle, showScale = true, currentLabel = R.string.label_current_sr, targetLabel = R.string.label_target_sr, group = Group.SW),
    UniCard(Product.REEF_BUFFER, R.string.calc_reef_buffer_title, R.string.calc_reef_buffer_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_alk, targetLabel = R.string.label_target_alk, group = Group.SW),
    UniCard(Product.REEF_BUILDER, R.string.calc_reef_builder_title, R.string.calc_reef_builder_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_alk, targetLabel = R.string.label_target_alk, group = Group.SW),
    UniCard(Product.REEF_CALCIUM, R.string.calc_reef_calcium_title, R.string.calc_reef_calcium_subtitle, showScale = true, currentLabel = R.string.label_current_ca, targetLabel = R.string.label_target_ca, group = Group.SW),
    UniCard(Product.REEF_CARBONATE, R.string.calc_reef_carbonate_title, R.string.calc_reef_carbonate_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_alk, targetLabel = R.string.label_target_alk, group = Group.SW),
    UniCard(Product.REEF_COMPLETE, R.string.calc_reef_complete_title, R.string.calc_reef_complete_subtitle, showScale = true, currentLabel = R.string.label_current_ca, targetLabel = R.string.label_target_ca, group = Group.SW),
    UniCard(Product.REEF_FUSION_1, R.string.calc_reef_fusion1_title, R.string.calc_reef_fusion1_subtitle, showScale = true, currentLabel = R.string.label_current_ca, targetLabel = R.string.label_target_ca, group = Group.SW),
    UniCard(Product.REEF_FUSION_2, R.string.calc_reef_fusion2_title, R.string.calc_reef_fusion2_subtitle, showScale = true, defaultScale = UnitScale.DKH, currentLabel = R.string.label_current_alk, targetLabel = R.string.label_target_alk, group = Group.SW),
    UniCard(Product.REEF_IODIDE, R.string.calc_reef_iodide_title, R.string.calc_reef_iodide_subtitle, showScale = true, currentLabel = R.string.label_current_i, targetLabel = R.string.label_target_i, group = Group.SW),
    UniCard(Product.REEF_STRONTIUM, R.string.calc_reef_strontium_title, R.string.calc_reef_strontium_subtitle, showScale = true, currentLabel = R.string.label_current_sr, targetLabel = R.string.label_target_sr, group = Group.SW),
)

/**
 * Compose port of fragment_calculators.xml (ADR-001). The 24 hand-wired XML cards
 * collapse into one reusable [CalcCard] driven by [UNI_CARDS]; calc logic stays in
 * the ViewModel ([MainViewModel.calculateUniversal] etc.). Volume is edited on the
 * Dashboard now; this screen shows the effective volume read-only.
 */
@Composable
fun CalculatorsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val profile by viewModel.profile.observeAsState(AquariumProfile.FRESHWATER)
    // Observe volume inputs so dose results recompute when the tank volume changes.
    viewModel.volume.observeAsState().value
    viewModel.volumeUnit.observeAsState().value
    viewModel.volumeMode.observeAsState().value
    viewModel.dimLength.observeAsState().value
    viewModel.dimBreadth.observeAsState().value
    viewModel.dimHeight.observeAsState().value
    viewModel.dimUnit.observeAsState().value
    val litres = viewModel.getEffectiveVolumeLitres()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(stringResource(R.string.nav_calculators), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.calc_volume_label, String.format("%.1f", litres)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

        when (profile) {
            AquariumProfile.FRESHWATER -> {
                UNI_CARDS.filter { it.group == Group.FW }.forEach { CalcCard(viewModel, it, litres) }
                QuickDoses(viewModel, litres)
            }
            AquariumProfile.SALTWATER -> {
                UNI_CARDS.filter { it.group == Group.SW }.forEach { CalcCard(viewModel, it, litres) }
                SaltMixCard(viewModel)
                QuickDoses(viewModel, litres)
            }
            AquariumProfile.POND -> {
                SubstrateCard(viewModel)
                QuickDoses(viewModel, litres)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    subtitle: String?,
    resultLine: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        Modifier.fillMaxWidth().padding(top = 10.dp).clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(resultLine, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            AnimatedVisibility(expanded) { Column { Spacer(Modifier.height(12.dp)); content() } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalcCard(viewModel: MainViewModel, spec: UniCard, litres: Double) {
    var current by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val scaleOptions = remember(spec) {
        if (spec.showScale) {
            val degree = if (spec.product == Product.EQUILIBRIUM) "dGH" else "dKH"
            listOf("meq/L", degree, "ppm")
        } else emptyList()
    }
    var scaleSel by remember(spec) {
        mutableStateOf(
            when (spec.defaultScale) { UnitScale.MEQ_L -> scaleOptions.getOrElse(0) { "ppm" }; UnitScale.DKH -> scaleOptions.getOrElse(1) { "ppm" }; else -> "ppm" }
        )
    }
    val scale = when {
        scaleSel.contains("meq") -> UnitScale.MEQ_L
        scaleSel.contains("dKH") || scaleSel.contains("dGH") -> UnitScale.DKH
        else -> UnitScale.PPM
    }

    val result = remember(current, target, scaleSel, litres) {
        viewModel.calculateUniversal(spec.product, current.toDoubleOrNull() ?: 0.0, target.toDoubleOrNull() ?: 0.0, scale)
    }
    val resultLine = if (result.primaryValue.toDouble() > 0.0) "${result.primaryValue.toPlainString()} ${result.primaryUnit}"
    else stringResource(R.string.result_no_dose)

    ExpandableCard(stringResource(spec.title), stringResource(spec.subtitle), resultLine) {
        if (spec.showScale && scaleOptions.isNotEmpty()) {
            Dropdown(scaleOptions, scaleOptions.indexOf(scaleSel).coerceAtLeast(0)) { scaleSel = scaleOptions[it] }
            Spacer(Modifier.height(8.dp))
        }
        if (spec.showInputs) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text(stringResource(spec.currentLabel)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(target, { target = it }, label = { Text(stringResource(spec.targetLabel)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        if (result.primaryValue.toDouble() > 0.0) {
            Text(stringResource(R.string.result_alternate, result.secondaryValue.toPlainString(), result.secondaryUnit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { options.firstOrNull().orEmpty() },
            onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, label -> DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(i); expanded = false }) }
        }
    }
}

@Composable
private fun QuickDoses(viewModel: MainViewModel, litres: Double) {
    val wcDefault by viewModel.defaultWaterChangePercent.observeAsState(20.0)
    ExpandableCard("Prime", null, stringResource(R.string.result_ml, String.format("%.1f", viewModel.calculatePrimeDose())), content = {})
    ExpandableCard("Stability", null, stringResource(R.string.result_ml, String.format("%.1f", viewModel.calculateStabilityDose())), content = {})
    ExpandableCard("Safe", null, stringResource(R.string.result_grams, String.format("%.2f", viewModel.calculateSafeSimple())), content = {})
    var pct by remember { mutableStateOf(wcDefault.toInt().toString()) }
    ExpandableCard("Water Change", null, stringResource(R.string.result_change_volume, String.format("%.1f", viewModel.calculateWaterChangeLitres(pct.toDoubleOrNull() ?: 0.0))), initiallyExpanded = true) {
        OutlinedTextField(pct, { pct = it }, label = { Text("%") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(120.dp))
    }
}

@Composable
private fun SubstrateCard(viewModel: MainViewModel) {
    val products = listOf("Flourite", "Flourite Black", "Flourite Black Sand", "Flourite Dark", "Flourite Red", "Flourite Sand", "Gray Coast", "Meridian", "Onyx", "Onyx Sand", "Pearl Beach")
    var l by remember { mutableStateOf("") }
    var w by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    var prodIdx by remember { mutableStateOf((viewModel.subProduct.value ?: 0).coerceIn(products.indices)) }
    viewModel.setSubLength(l.toDoubleOrNull() ?: 0.0); viewModel.setSubWidth(w.toDoubleOrNull() ?: 0.0); viewModel.setSubDepth(d.toDoubleOrNull() ?: 0.0)
    val res = remember(l, w, d, prodIdx) { viewModel.calculateSubstrate(prodIdx) }
    val bags = res.primaryValue.toDouble()
    ExpandableCard(stringResource(R.string.calc_substrate_title), products[prodIdx], stringResource(R.string.result_substrate_bags, res.primaryValue.toPlainString(), String.format("%.1f", bags * 7.0)), initiallyExpanded = true) {
        Dropdown(products, prodIdx) { prodIdx = it }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(l, { l = it }, label = { Text("L") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(w, { w = it }, label = { Text("W") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(d, { d = it }, label = { Text("D") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SaltMixCard(viewModel: MainViewModel) {
    val products = remember { com.example.seachem_dosing.logic.SaltMixCalculations.SALT_MIX_PRODUCTS.keys.toList() }
    var prodIdx by remember { mutableStateOf((viewModel.saltMixProduct.value ?: 0).coerceIn(products.indices.takeIf { products.isNotEmpty() } ?: 0..0)) }
    var vol by remember { mutableStateOf("") }
    var cur by remember { mutableStateOf("") }
    var tar by remember { mutableStateOf("") }
    viewModel.setSaltMixProduct(prodIdx)
    viewModel.setSaltMixVolume(vol.toDoubleOrNull() ?: 0.0)
    viewModel.setSaltMixCurrentPpt(cur.toDoubleOrNull() ?: 0.0)
    viewModel.setSaltMixDesiredPpt(tar.toDoubleOrNull() ?: 0.0)
    val res = remember(prodIdx, vol, cur, tar) { viewModel.calculateSaltMix() }
    val resultLine = if (res != null) "${res.kilograms} kg" else stringResource(R.string.result_placeholder)
    ExpandableCard(stringResource(R.string.calc_salt_mix_title), if (products.isNotEmpty()) products[prodIdx] else null, resultLine, initiallyExpanded = true) {
        if (products.isNotEmpty()) { Dropdown(products, prodIdx) { prodIdx = it }; Spacer(Modifier.height(8.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(vol, { vol = it }, label = { Text(stringResource(R.string.label_volume)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(cur, { cur = it }, label = { Text(stringResource(R.string.label_current)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(tar, { tar = it }, label = { Text(stringResource(R.string.label_target)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        if (res != null) { Spacer(Modifier.height(6.dp)); Text("${res.grams} g (${res.pounds} lbs)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
