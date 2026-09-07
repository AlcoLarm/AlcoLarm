package com.alcolarm.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alcolarm.app.R
import com.alcolarm.core.data.UserPreferencesRepository
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisclaimerViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    val accepted: StateFlow<Boolean> = repository.disclaimerAccepted.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    fun accept(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setDisclaimerAccepted(true)
            onDone()
        }
    }
}

@Composable
fun DisclaimerRoute(
    requireAccept: Boolean,
    onAcceptedOrClosed: () -> Unit,
    viewModel: DisclaimerViewModel = hiltViewModel(),
) {
    val accepted by viewModel.accepted.collectAsStateWithLifecycle()
    BackHandler(enabled = requireAccept && !accepted) { /* must accept on first launch */ }
    BackHandler(enabled = !requireAccept) { onAcceptedOrClosed() }

    DisclaimerScreen(
        requireAccept = requireAccept,
        onAccept = { viewModel.accept(onAcceptedOrClosed) },
        onClose = onAcceptedOrClosed,
    )
}

@Composable
fun DisclaimerScreen(
    requireAccept: Boolean,
    onAccept: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.headlineLarge,
            color = ClearSignalColors.OnDark,
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.disclaimer_body),
                style = MaterialTheme.typography.bodyLarge,
                color = ClearSignalColors.OnDarkMuted,
            )
        }
        Spacer(Modifier.height(16.dp))
        if (requireAccept) {
            SignalPrimaryButton(
                text = stringResource(R.string.disclaimer_accept),
                onClick = onAccept,
            )
        } else {
            SignalPrimaryButton(
                text = stringResource(R.string.disclaimer_close),
                onClick = onClose,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
