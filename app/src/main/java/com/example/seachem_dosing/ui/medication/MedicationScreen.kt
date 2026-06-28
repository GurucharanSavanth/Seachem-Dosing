package com.example.seachem_dosing.ui.medication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.core.result.CalcResult
import com.example.seachem_dosing.domain.medication.MedProduct
import com.example.seachem_dosing.domain.medication.MedicationCatalog
import com.example.seachem_dosing.domain.medication.MedicationSafetyEngine
import com.example.seachem_dosing.domain.medication.MedicationSafetyEngine.MedAdvice
import com.example.seachem_dosing.domain.medication.MedicationSafetyEngine.TankContext
import com.example.seachem_dosing.domain.medication.WaterType
import java.math.BigDecimal

// ponytail: English literals for this new module's labels; extract to strings.xml + values-kn when localizing (debt noted).
private val WATER_TYPES = listOf(WaterType.FRESHWATER to "Freshwater", WaterType.SALTWATER to "Saltwater", WaterType.BRACKISH to "Brackish")
private val SYMPTOMS = listOf(
    "White spots", "Fuzzy/cotton growth", "Fin damage/rot", "Rapid breathing", "Flashing/scratching",
    "Lethargy", "Bloating", "Loss of appetite", "Red/inflamed areas", "Cloudy eyes",
)

private const val DISCLAIMER =
    "Decision support, not a veterinary diagnosis. Confirm species, test your water, and consult an aquatic vet or the manufacturer label before treating. Antibiotics, copper, formaldehyde and malachite green are high-risk."

@Composable
fun MedicationScreen(modifier: Modifier = Modifier) {
    var expert by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Medication", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        WarningCard(DISCLAIMER)
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !expert, onClick = { expert = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Beginner") }
            SegmentedButton(selected = expert, onClick = { expert = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Expert") }
        }
        Spacer(Modifier.height(12.dp))
        if (expert) ExpertFlow() else BeginnerFlow()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WarningCard(text: String) {
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(text, Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

// ---------------- Expert ----------------

@Composable
private fun ExpertFlow() {
    var waterType by remember { mutableStateOf(WaterType.FRESHWATER) }
    val products = remember(waterType) { MedicationCatalog.forWaterType(waterType) }
    var productIdx by remember(waterType) { mutableStateOf(0) }
    var volume by remember { mutableStateOf("") }
    var inverts by remember { mutableStateOf(false) }
    var filtration by remember { mutableStateOf(false) }
    var species by remember { mutableStateOf(false) }
    var assessed by remember { mutableStateOf<CalcResult<MedAdvice>?>(null) }

    LabeledDropdown("Water type", WATER_TYPES.map { it.second }, WATER_TYPES.indexOfFirst { it.first == waterType }) {
        waterType = WATER_TYPES[it].first; assessed = null
    }
    Spacer(Modifier.height(8.dp))
    if (products.isNotEmpty()) {
        LabeledDropdown("Product", products.map { "${it.brand} ${it.name}" }, productIdx.coerceIn(products.indices)) { productIdx = it; assessed = null }
    } else {
        Text("No catalogued products verified for this water type.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(volume, { volume = it }, label = { Text("Tank volume (L)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    SwitchRow("Invertebrates / corals present", inverts) { inverts = it }
    SwitchRow("Carbon/UV/filtration acknowledged", filtration) { filtration = it }
    SwitchRow("Species confirmed", species) { species = it }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Assess safety", enabled = products.isNotEmpty()) {
        val product = products[productIdx.coerceIn(products.indices)]
        assessed = MedicationSafetyEngine.assess(
            product,
            TankContext(
                waterType = waterType,
                volumeLitres = volume.toBigDecimalOrNull(),
                hasInvertsOrCorals = inverts,
                filtrationAcknowledged = filtration,
                speciesConfirmed = species,
            ),
        )
    }
    assessed?.let { AdviceCard(it) }
}

private fun String.toBigDecimalOrNull(): BigDecimal? = trim().takeIf { it.isNotEmpty() }?.let { runCatching { BigDecimal(it) }.getOrNull() }

@Composable
private fun AdviceCard(result: CalcResult<MedAdvice>) {
    val (containerColor, title, lines) = when (result) {
        is CalcResult.Success -> Triple(MaterialTheme.colorScheme.primaryContainer, "Dosing guidance",
            listOf("Dose: ${result.value.doseRule}") + result.value.warnings)
        is CalcResult.NeedsMoreInput -> Triple(MaterialTheme.colorScheme.secondaryContainer, "More info needed",
            listOf(result.reason) + result.required.map { "• $it" })
        is CalcResult.UnsafeBlocked -> Triple(MaterialTheme.colorScheme.errorContainer, "Not safe — blocked",
            listOfNotNull(result.reason, result.evidence, result.escalation))
        is CalcResult.Unsupported -> Triple(MaterialTheme.colorScheme.surfaceVariant, "Unsupported", listOfNotNull(result.reason, result.evidenceGap))
        is CalcResult.CalculationError -> Triple(MaterialTheme.colorScheme.errorContainer, "Error", listOf(result.debugMessage))
    }
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            lines.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp)) }
        }
    }
}

// ---------------- Beginner ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BeginnerFlow() {
    var waterType by remember { mutableStateOf(WaterType.FRESHWATER) }
    var inverts by remember { mutableStateOf(false) }
    val selectedSymptoms = remember { mutableListOf<String>().toMutableStateList() }
    var showOptions by remember { mutableStateOf(false) }

    LabeledDropdown("Water type", WATER_TYPES.map { it.second }, WATER_TYPES.indexOfFirst { it.first == waterType }) { waterType = WATER_TYPES[it].first; showOptions = false }
    SwitchRow("Invertebrates / corals present", inverts) { inverts = it; showOptions = false }
    Spacer(Modifier.height(8.dp))
    Text("What do you observe?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SYMPTOMS.forEach { s ->
            FilterChip(
                selected = s in selectedSymptoms,
                onClick = { if (s in selectedSymptoms) selectedSymptoms.remove(s) else selectedSymptoms.add(s) },
                label = { Text(s) },
                colors = FilterChipDefaults.filterChipColors(),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    PrimaryButton("Show compatible options") { showOptions = true }

    if (showOptions) {
        Spacer(Modifier.height(8.dp))
        // Safety-first: never diagnose from symptoms. Show products the LABEL verifies for this
        // water type, each run through the safety engine with the known context. Low-confidence by design.
        Text("Not a diagnosis. Products verified for ${waterType.name.lowercase()}; confirm species + consult an expert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MedicationCatalog.forWaterType(waterType).forEach { product ->
            val result = MedicationSafetyEngine.assess(
                product,
                TankContext(waterType = waterType, volumeLitres = BigDecimal("1"), hasInvertsOrCorals = inverts, filtrationAcknowledged = false, speciesConfirmed = false),
            )
            BeginnerProductRow(product, result)
        }
    }
}

@Composable
private fun BeginnerProductRow(product: MedProduct, result: CalcResult<MedAdvice>) {
    val status = when (result) {
        is CalcResult.UnsafeBlocked -> "Blocked: ${result.reason}"
        is CalcResult.NeedsMoreInput -> "Needs expert confirmation (${result.required.joinToString()})"
        is CalcResult.Success -> "Compatible — verify dose on label"
        else -> "Unsupported"
    }
    val color = if (result is CalcResult.UnsafeBlocked) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(14.dp)) {
            Text("${product.brand} ${product.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("${product.category} · ${product.actives.joinToString()} · ${product.evidence}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ---------------- shared bits ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(
            value = options.getOrElse(selectedIndex) { "" }, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(i); expanded = false }) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
