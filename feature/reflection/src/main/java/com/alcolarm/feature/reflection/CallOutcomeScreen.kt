package com.alcolarm.feature.reflection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.component.SignalUrgentButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors

@Composable
fun CallOutcomeRoute(
    contactName: String,
    onReachedThem: () -> Unit,
    onDidNotAnswer: () -> Unit,
) {
    BackHandler { onDidNotAnswer() }
    CallOutcomeScreen(
        contactName = contactName,
        onReachedThem = onReachedThem,
        onDidNotAnswer = onDidNotAnswer,
    )
}

@Composable
fun CallOutcomeScreen(
    contactName: String,
    onReachedThem: () -> Unit,
    onDidNotAnswer: () -> Unit,
) {
    val label = contactName.trim().ifBlank { stringResource(R.string.call_outcome_them) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.call_outcome_title),
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.call_outcome_body, label),
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.weight(1f))
        SignalUrgentButton(
            text = stringResource(R.string.call_outcome_reached),
            onClick = onReachedThem,
        )
        Spacer(Modifier.height(12.dp))
        SignalPrimaryButton(
            text = stringResource(R.string.call_outcome_no_answer),
            onClick = onDidNotAnswer,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.call_outcome_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = ClearSignalColors.OnDarkMuted,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ReachedPraiseRoute(onDone: () -> Unit) {
    BackHandler { onDone() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.reflection_reached_title),
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.reflection_reached_body),
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.weight(1f))
        SignalPrimaryButton(
            text = stringResource(R.string.call_outcome_back_home),
            onClick = onDone,
        )
        Spacer(Modifier.height(24.dp))
    }
}
