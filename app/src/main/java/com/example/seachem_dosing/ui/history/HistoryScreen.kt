package com.example.seachem_dosing.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.data.local.entity.HistoryTimelineRow
import com.example.seachem_dosing.domain.history.HistoryEventType
import com.example.seachem_dosing.domain.history.ParameterType
import java.text.DateFormat
import java.util.Date

private val FILTERS: List<Pair<String, HistoryEventType?>> = listOf(
    "All" to null,
    "Doses" to HistoryEventType.DOSE_ADMINISTERED,
    "Readings" to HistoryEventType.WATER_PARAMETER_RECORDED,
)

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    selectedType: HistoryEventType?,
    onSelectType: (HistoryEventType?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "History",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTERS.forEach { (label, type) ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (state) {
            is HistoryUiState.Loading -> Centered { CircularProgressIndicator() }
            is HistoryUiState.Empty -> Centered {
                Text(
                    "No history yet. Logged doses and saved readings will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is HistoryUiState.Error -> Centered {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
            is HistoryUiState.Content -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.rows, key = { it.event.eventId }) { row ->
                    HistoryRowCard(row, voided = row.event.eventId in state.voidedEventIds)
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        content()
    }
}

@Composable
private fun HistoryRowCard(row: HistoryTimelineRow, voided: Boolean) {
    val container = if (voided) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    eventLabel(row.event.eventTypeCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (voided) {
                    Text("VOIDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                formatTime(row.event.occurredAtEpochMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            detailLine(row)?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private fun detailLine(row: HistoryTimelineRow): String? {
    row.dose?.let { d ->
        val amount = d.administeredAmountDecimal ?: d.calculatedAmountDecimal
        val unit = d.administeredAmountUnitCode ?: d.calculatedAmountUnitCode ?: ""
        val product = d.legacyProductLabel ?: d.productId ?: "—"
        return listOfNotNull(product, amount?.let { "$it $unit".trim() }).joinToString(" · ")
    }
    row.parameter?.let { p ->
        val type = ParameterType.fromCode(p.parameterTypeCode)?.name?.lowercase() ?: p.parameterTypeCode
        return "$type ${p.measuredValueDecimal} ${p.measuredUnitCode}"
    }
    return null
}

private fun eventLabel(code: String): String = when (HistoryEventType.fromCode(code)) {
    HistoryEventType.DOSE_ADMINISTERED -> "Dose administered"
    HistoryEventType.WATER_PARAMETER_RECORDED -> "Reading recorded"
    HistoryEventType.LEGACY_DOSE_ADMINISTERED -> "Dose administered (legacy)"
    HistoryEventType.LEGACY_DOSE_CALCULATION -> "Dose calculation (legacy)"
    HistoryEventType.LEGACY_PARAMETER_RECORD -> "Reading (legacy)"
    HistoryEventType.LEGACY_PARAMETER_SNAPSHOT_EMPTY -> "Empty snapshot (legacy)"
    HistoryEventType.CORRECTION -> "Correction"
    HistoryEventType.VOID -> "Void"
    null -> code
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
