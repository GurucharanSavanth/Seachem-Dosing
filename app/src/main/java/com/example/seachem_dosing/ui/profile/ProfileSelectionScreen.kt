package com.example.seachem_dosing.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel.AquariumProfile
import com.example.seachem_dosing.ui.theme.ProfileFreshwater
import com.example.seachem_dosing.ui.theme.ProfilePond
import com.example.seachem_dosing.ui.theme.ProfileSaltwater

private data class ProfileOption(
    val profile: AquariumProfile,
    val letter: String,
    @StringRes val title: Int,
    @StringRes val desc: Int,
    @StringRes val features: Int,
    val accent: Color,
)

private val PROFILE_OPTIONS = listOf(
    ProfileOption(AquariumProfile.FRESHWATER, "F", R.string.profile_freshwater, R.string.profile_freshwater_desc, R.string.profile_freshwater_features, ProfileFreshwater),
    ProfileOption(AquariumProfile.SALTWATER, "S", R.string.profile_saltwater, R.string.profile_saltwater_desc, R.string.profile_saltwater_features, ProfileSaltwater),
    // POND id preserved (CLAUDE.md); rendered via R.string.profile_pond ("Sand and Gravel").
    ProfileOption(AquariumProfile.POND, "P", R.string.profile_pond, R.string.profile_pond_desc, R.string.profile_pond_features, ProfilePond),
)

/**
 * Compose port of fragment_profile_selection.xml (ADR-001, parity-gated).
 * Stateless apart from the in-progress selection; persistence + navigation stay
 * in [ProfileSelectionFragment] via [onContinue], exactly as the XML version did.
 */
@Composable
fun ProfileSelectionScreen(
    initialSelected: AquariumProfile?,
    onContinue: (AquariumProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember { mutableStateOf(initialSelected) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.profile_select_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.profile_select_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        PROFILE_OPTIONS.forEach { option ->
            ProfileOptionCard(
                option = option,
                selected = selected == option.profile,
                onClick = { selected = option.profile },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { selected?.let(onContinue) },
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.profile_select_continue))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileOptionCard(
    option: ProfileOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (selected) BorderStroke(2.dp, option.accent) else null,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = option.accent, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option.letter, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(option.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(option.desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(option.features), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
