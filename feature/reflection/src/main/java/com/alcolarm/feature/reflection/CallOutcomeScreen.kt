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
    val label = contactName.trim().ifBlank { "them" }
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
            text = "How did it go?",
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You opened the dialer for $label. Tell us what happened — " +
                "we’ll meet you where you are.",
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.weight(1f))
        SignalUrgentButton(
            text = "I reached them",
            onClick = onReachedThem,
        )
        Spacer(Modifier.height(12.dp))
        SignalPrimaryButton(
            text = "They didn’t answer",
            onClick = onDidNotAnswer,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "If you’re not sure, choose “They didn’t answer” — " +
                "we’ll still celebrate that you tried.",
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
            text = ReflectionCopy.REACHED_TITLE,
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = ReflectionCopy.REACHED_BODY,
            style = MaterialTheme.typography.bodyLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.weight(1f))
        SignalPrimaryButton(text = "Back to Home", onClick = onDone)
        Spacer(Modifier.height(24.dp))
    }
}
