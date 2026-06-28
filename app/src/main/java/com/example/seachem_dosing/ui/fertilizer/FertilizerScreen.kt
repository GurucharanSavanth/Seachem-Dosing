package com.example.seachem_dosing.ui.fertilizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.core.result.CalcResult
import com.example.seachem_dosing.domain.engine.FertilizerChemistryEngine
import com.example.seachem_dosing.domain.engine.FertilizerChemistryEngine.PpmResult
import java.math.BigDecimal
import java.math.RoundingMode

// ponytail: English literals; extract to strings.xml + values-kn when localizing (debt noted, same as Medication).
private const val SEPARATION =
    "Keep MACRO (N, P, K) and MICRO/trace (Fe) stock solutions in SEPARATE bottles — iron + phosphate precipitate as FePO₄, and concentrated mixes grow microbes."

private val COMPOUNDS = FertilizerChemistryEngine.CATALOG.values.toList()

@Composable
fun FertilizerScreen(modifier: Modifier = Modifier) {
    var expert by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("DIY Fertilizer", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        InfoCard(SEPARATION)
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !expert, onClick = { expert = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Beginner") }
            SegmentedButton(selected = expert, onClick = { expert = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Expert") }
        }
        Spacer(Modifier.height(12.dp))
        if (expert) ExpertCalculator() else BeginnerGuide()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun ExpertCalculator() {
    var compoundIdx by remember { mutableStateOf(0) }
    val compound = COMPOUNDS[compoundIdx.coerceIn(COMPOUNDS.indices)]
    var targetMode by remember { mutableStateOf(false) } // false = dose→ppm, true = target ppm→grams
    var volume by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var targetPpm by remember { mutableStateOf("") }
    var nutrientIdx by remember(compound) { mutableStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }

    Dropdown("Compound", COMPOUNDS.map { it.formula }, compoundIdx) { compoundIdx = it; result = null }
    Spacer(Modifier.height(8.dp))
    Text("Molar mass ${compound.molarMass} g/mol · ${compound.nutrients.joinToString { "${it.symbol} ${(it.massFraction.movePointRight(2)).setScale(1, RoundingMode.HALF_UP)}%" }}",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        SegmentedButton(selected = !targetMode, onClick = { targetMode = false; result = null }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Dose → ppm") }
        SegmentedButton(selected = targetMode, onClick = { targetMode = true; result = null }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Target → grams") }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(volume, { volume = it }, label = { Text("Tank volume (L)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    if (targetMode) {
        Dropdown("Nutrient", compound.nutrients.map { it.symbol }, nutrientIdx.coerceIn(compound.nutrients.indices)) { nutrientIdx = it; result = null }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(targetPpm, { targetPpm = it }, label = { Text("Target increase (ppm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    } else {
        OutlinedTextField(dose, { dose = it }, label = { Text("Dose (grams)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    }
    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        val vol = volume.toBd()
        result = if (targetMode) {
            val nutrient = compound.nutrients.getOrNull(nutrientIdx.coerceIn(compound.nutrients.indices))?.symbol ?: ""
            renderBd(FertilizerChemistryEngine.gramsForTargetPpm(compound.id, nutrient, targetPpm.toBd() ?: BigDecimal.ZERO, vol ?: BigDecimal.ZERO), "g of ${compound.formula}")
        } else {
            renderPpm(FertilizerChemistryEngine.ppmIncrease(compound.id, dose.toBd() ?: BigDecimal.ZERO, vol ?: BigDecimal.ZERO))
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("Calculate") }
    result?.let { ResultCard(it) }
}

private fun String.toBd(): BigDecimal? = trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }

private fun renderPpm(r: CalcResult<List<PpmResult>>): String = when (r) {
    is CalcResult.Success -> r.value.joinToString("\n") { "${it.nutrient}: +${it.ppm.setScale(3, RoundingMode.HALF_UP)} mg/L" }
    is CalcResult.NeedsMoreInput -> "Need: ${r.required.joinToString()} — ${r.reason}"
    is CalcResult.Unsupported -> "Unsupported: ${r.reason}"
    is CalcResult.CalculationError -> "Error: ${r.debugMessage}"
    is CalcResult.UnsafeBlocked -> r.reason
}

private fun renderBd(r: CalcResult<BigDecimal>, unit: String): String = when (r) {
    is CalcResult.Success -> "${r.value.setScale(3, RoundingMode.HALF_UP)} $unit"
    is CalcResult.NeedsMoreInput -> "Need: ${r.required.joinToString()} — ${r.reason}"
    is CalcResult.Unsupported -> "Unsupported: ${r.reason}"
    is CalcResult.CalculationError -> "Error: ${r.debugMessage}"
    is CalcResult.UnsafeBlocked -> r.reason
}

@Composable
private fun ResultCard(text: String) {
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text("Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun BeginnerGuide() {
    Text("Compounds by group", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
    FertilizerChemistryEngine.Category.entries.forEach { cat ->
        val items = COMPOUNDS.filter { it.category == cat }
        if (items.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text(cat.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    items.forEach { c ->
                        Text("${c.formula} — ${c.nutrients.joinToString { it.symbol }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Switch to Expert to compute exact grams or ppm. Provide tank volume + a measured/target value.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" }, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); expanded = false }) }
        }
    }
}
