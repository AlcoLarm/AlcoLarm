package com.alcolarm.feature.riskplaces

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcolarm.core.designsystem.component.SignalChip
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.RiskPlaceId

@Composable
fun RiskPlacesRoute(
    onContinue: () -> Unit,
    editMode: Boolean = false,
    viewModel: RiskPlacesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RiskPlacesScreen(
        state = state,
        editMode = editMode,
        onToggle = viewModel::toggle,
        onContinue = { viewModel.saveAndContinue(onContinue) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RiskPlacesScreen(
    state: RiskPlacesUiState,
    onToggle: (RiskPlaceId) -> Unit,
    onContinue: () -> Unit,
    editMode: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.risk_places_title),
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.risk_places_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RiskPlaceId.entries.forEach { id ->
                SignalChip(
                    label = stringResource(id.labelRes),
                    selected = id in state.selected,
                    onClick = { onToggle(id) },
                )
            }
        }
        Spacer(Modifier.weight(1f))
        SignalPrimaryButton(
            text = if (editMode) {
                stringResource(R.string.action_save)
            } else {
                stringResource(R.string.action_continue)
            },
            onClick = onContinue,
            enabled = state.selected.isNotEmpty(),
        )
        Spacer(Modifier.height(16.dp))
    }
}
