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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import java.io.File

private enum class ReflectionPage {
    Affirmation,
    TurnAround,
    DrinkAgain,
    Closing,
}

@Composable
fun ReflectionRoute(
    mode: ReflectionMode,
    showAffirmationFirst: Boolean,
    onFinished: () -> Unit,
    onSkip: () -> Unit,
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val photos by viewModel.photoFiles.collectAsStateWithLifecycle()
    val answers by viewModel.answers.collectAsStateWithLifecycle()
    val prefillReady by viewModel.prefillReady.collectAsStateWithLifecycle()

    ReflectionScreen(
        photoFiles = photos,
        answers = answers,
        prefillReady = prefillReady,
        mode = mode,
        showAffirmationFirst = showAffirmationFirst,
        onTurnAroundChanged = viewModel::onTurnAroundChanged,
        onDrinkAgainChanged = viewModel::onDrinkAgainChanged,
        onPersistTurnAround = viewModel::persistTurnAround,
        onPersistDrinkAgain = viewModel::persistDrinkAgain,
        onPersistBoth = viewModel::persistBoth,
        onFinished = onFinished,
        onSkip = onSkip,
    )
}

@Composable
fun ReflectionScreen(
    photoFiles: List<File>,
    answers: ReflectionAnswers,
    prefillReady: Boolean,
    mode: ReflectionMode,
    showAffirmationFirst: Boolean,
    onTurnAroundChanged: (String) -> Unit,
    onDrinkAgainChanged: (String) -> Unit,
    onPersistTurnAround: () -> Unit,
    onPersistDrinkAgain: () -> Unit,
    onPersistBoth: () -> Unit,
    onFinished: () -> Unit,
    onSkip: () -> Unit,
) {
    val pages = buildList {
        if (showAffirmationFirst) add(ReflectionPage.Affirmation)
        add(ReflectionPage.TurnAround)
        add(ReflectionPage.DrinkAgain)
        add(ReflectionPage.Closing)
    }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = pages[pageIndex.coerceIn(0, pages.lastIndex)]
    val allowSkip = mode == ReflectionMode.OPTIONAL
    val isLast = pageIndex >= pages.lastIndex

    BackHandler(enabled = !allowSkip) { /* mandatory: stay */ }
    BackHandler(enabled = allowSkip) { onSkip() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearSignalColors.NearBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Text(
            text = when (page) {
                ReflectionPage.Affirmation -> stringResource(R.string.reflection_affirmation_header)
                else -> stringResource(R.string.reflection_screen_title)
            },
            style = MaterialTheme.typography.labelLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            if (photoFiles.isNotEmpty() && page != ReflectionPage.Affirmation) {
                val context = LocalContext.current
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoFiles.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.reflection_cd_loved_ones_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.height(16.dp))
            }

            when (page) {
                ReflectionPage.Affirmation -> {
                    Text(
                        text = stringResource(R.string.reflection_affirmation_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = ClearSignalColors.OnDark,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.reflection_affirmation_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ClearSignalColors.OnDarkMuted,
                    )
                }
                ReflectionPage.TurnAround -> {
                    QuestionPage(
                        question = stringResource(R.string.reflection_question_turn_around),
                        value = answers.turnAround,
                        onValueChange = onTurnAroundChanged,
                        placeholder = stringResource(R.string.reflection_turn_around_placeholder),
                        ready = prefillReady,
                    )
                }
                ReflectionPage.DrinkAgain -> {
                    QuestionPage(
                        question = stringResource(R.string.reflection_question_drink_again),
                        value = answers.drinkAgain,
                        onValueChange = onDrinkAgainChanged,
                        placeholder = stringResource(R.string.reflection_drink_again_placeholder),
                        ready = prefillReady,
                    )
                }
                ReflectionPage.Closing -> {
                    Text(
                        text = stringResource(R.string.reflection_closing_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = ClearSignalColors.OnDark,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.reflection_closing_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = ClearSignalColors.OnDarkMuted,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val canContinue = when (page) {
            ReflectionPage.TurnAround -> answers.turnAround.trim().isNotEmpty()
            ReflectionPage.DrinkAgain -> answers.drinkAgain.trim().isNotEmpty()
            ReflectionPage.Affirmation, ReflectionPage.Closing -> true
        }

        SignalPrimaryButton(
            text = when {
                isLast -> stringResource(R.string.reflection_im_ready)
                else -> stringResource(R.string.reflection_continue)
            },
            enabled = canContinue,
            onClick = {
                when (page) {
                    ReflectionPage.TurnAround -> onPersistTurnAround()
                    ReflectionPage.DrinkAgain -> {
                        onPersistDrinkAgain()
                        onPersistBoth()
                    }
                    ReflectionPage.Closing -> onPersistBoth()
                    ReflectionPage.Affirmation -> Unit
                }
                if (isLast) {
                    onFinished()
                } else {
                    pageIndex += 1
                }
            },
        )

        if (allowSkip) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.reflection_not_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = ClearSignalColors.OnDarkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.reflection_mandatory_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun QuestionPage(
    question: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    ready: Boolean,
) {
    Text(
        text = question,
        style = MaterialTheme.typography.headlineLarge,
        color = ClearSignalColors.OnDark,
    )
    Spacer(Modifier.height(16.dp))
    if (ready) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder)
            },
            minLines = 5,
            maxLines = 10,
            colors = reflectionFieldColors(),
        )
    }
}

@Composable
private fun reflectionFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClearSignalColors.SoftBlue,
    unfocusedBorderColor = ClearSignalColors.Outline,
    focusedTextColor = ClearSignalColors.OnDark,
    unfocusedTextColor = ClearSignalColors.OnDark,
    cursorColor = ClearSignalColors.SoftBlue,
    focusedContainerColor = ClearSignalColors.Surface,
    unfocusedContainerColor = ClearSignalColors.Surface,
    focusedPlaceholderColor = ClearSignalColors.OnDarkMuted,
    unfocusedPlaceholderColor = ClearSignalColors.OnDarkMuted,
)
