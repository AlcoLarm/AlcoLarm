package com.alcolarm.feature.reflection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alcolarm.core.designsystem.component.SignalPrimaryButton
import com.alcolarm.core.designsystem.theme.ClearSignalColors
import com.alcolarm.core.model.UserProfile
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ReflectionRoute(
    mode: ReflectionMode,
    showAffirmationFirst: Boolean,
    onFinished: () -> Unit,
    onSkip: () -> Unit,
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val photos by viewModel.photoFiles.collectAsStateWithLifecycle()

    ReflectionScreen(
        profile = profile,
        photoFiles = photos,
        mode = mode,
        showAffirmationFirst = showAffirmationFirst,
        onFinished = onFinished,
        onSkip = onSkip,
    )
}

@Composable
fun ReflectionScreen(
    profile: UserProfile,
    photoFiles: List<File>,
    mode: ReflectionMode,
    showAffirmationFirst: Boolean,
    onFinished: () -> Unit,
    onSkip: () -> Unit,
) {
    val cards = ReflectionCopy.buildCards(profile)
    val pages = buildList {
        if (showAffirmationFirst) {
            add(
                ReflectionCard(
                    title = ReflectionCopy.AFFIRMATION_TITLE,
                    body = ReflectionCopy.AFFIRMATION_BODY,
                ),
            )
        }
        addAll(cards)
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val allowSkip = mode == ReflectionMode.OPTIONAL
    val isLast = pagerState.currentPage >= pages.lastIndex

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
            text = if (showAffirmationFirst && pagerState.currentPage == 0) {
                "You reached out"
            } else {
                "A moment to reflect"
            },
            style = MaterialTheme.typography.labelLarge,
            color = ClearSignalColors.OnDarkMuted,
        )
        Spacer(Modifier.height(8.dp))

        if (photoFiles.isNotEmpty() && !(showAffirmationFirst && pagerState.currentPage == 0)) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoFiles.first())
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo of loved ones",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.height(16.dp))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            userScrollEnabled = allowSkip,
        ) { page ->
            val card = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = card.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = ClearSignalColors.OnDark,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = card.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ClearSignalColors.OnDarkMuted,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == pagerState.currentPage) ClearSignalColors.SoftBlue
                            else ClearSignalColors.Outline,
                        ),
                )
            }
        }

        if (isLast) {
            SignalPrimaryButton(text = "I’m ready", onClick = onFinished)
        } else {
            SignalPrimaryButton(
                text = "Continue",
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
            )
        }

        if (allowSkip) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Not now",
                    style = MaterialTheme.typography.labelLarge,
                    color = ClearSignalColors.OnDarkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Take your time — Continue when you’re ready.",
                style = MaterialTheme.typography.bodyMedium,
                color = ClearSignalColors.OnDarkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
