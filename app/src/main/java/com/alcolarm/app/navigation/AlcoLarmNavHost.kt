package com.alcolarm.app.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alcolarm.app.ui.SplashScreen
import com.alcolarm.feature.alert.AlertRoute
import com.alcolarm.feature.emergency.EmergencyRoute
import com.alcolarm.feature.location.HomeRoute
import com.alcolarm.feature.location.RiskAlertBus
import com.alcolarm.feature.onboarding.OnboardingRoute
import com.alcolarm.feature.reflection.CallOutcomeRoute
import com.alcolarm.feature.reflection.ReachedPraiseRoute
import com.alcolarm.feature.reflection.ReflectionMode
import com.alcolarm.feature.reflection.ReflectionRoute
import com.alcolarm.feature.riskplaces.RiskPlacesRoute
import com.alcolarm.core.data.UserPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RiskAlertBusEntryPoint {
    fun riskAlertBus(): RiskAlertBus
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProfileEntryPoint {
    fun userPreferencesRepository(): UserPreferencesRepository
}

@Composable
fun AlcoLarmNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startViewModel: StartDestinationViewModel = hiltViewModel(),
    openAlertRequested: Boolean = false,
    onOpenAlertConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val alertBus = EntryPointAccessors.fromApplication(
        context.applicationContext,
        RiskAlertBusEntryPoint::class.java,
    ).riskAlertBus()
    val profileRepo = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ProfileEntryPoint::class.java,
    ).userPreferencesRepository()
    val profile by profileRepo.profile.collectAsStateWithLifecycle(
        initialValue = com.alcolarm.core.model.UserProfile(),
    )

    fun goHomeClearingAlertStack() {
        navController.navigate(Routes.Home) {
            popUpTo(Routes.Home) { inclusive = false }
            launchSingleTop = true
        }
    }

    LaunchedEffect(navController) {
        alertBus.events.collect { event ->
            val current = navController.currentDestination?.route
            if (current != Routes.Alert) {
                navController.navigate(Routes.Alert) {
                    launchSingleTop = true
                }
            }
            Log.d(
                "AlcoLarm.Nav",
                "Risk alert navigate simulated=${event.simulated} risk=${event.riskPlaceId}",
            )
        }
    }

    LaunchedEffect(openAlertRequested) {
        if (!openAlertRequested) return@LaunchedEffect
        val current = navController.currentDestination?.route
        if (current != Routes.Alert) {
            if (current == Routes.Splash || current == null) {
                navController.navigate(Routes.Home) {
                    popUpTo(Routes.Splash) { inclusive = true }
                    launchSingleTop = true
                }
            }
            navController.navigate(Routes.Alert) {
                launchSingleTop = true
            }
        }
        onOpenAlertConsumed()
        Log.d("AlcoLarm.Nav", "Opened alert from full-screen / notification intent")
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
            SplashScreen()
            LaunchedEffect(Unit) {
                coroutineScope {
                    val completeDeferred = async { startViewModel.awaitOnboardingComplete() }
                    delay(700)
                    val complete = completeDeferred.await()
                    val dest = if (complete) Routes.Home else Routes.Onboarding
                    navController.navigate(dest) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.Onboarding) {
            OnboardingRoute(
                onContinue = {
                    navController.navigate(Routes.RiskPlaces)
                },
            )
        }
        composable(Routes.RiskPlaces) {
            RiskPlacesRoute(
                onContinue = {
                    navController.navigate(Routes.Emergency)
                },
            )
        }
        composable(Routes.Emergency) {
            EmergencyRoute(
                onContinue = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Home) {
            HomeRoute(
                onPauseReflect = {
                    navController.navigate(
                        Routes.reflection(mode = "optional", affirmation = false),
                    )
                },
            )
        }
        composable(Routes.Alert) {
            AlertRoute(
                onPauseReflect = {
                    alertBus.tryEmitDismissed()
                    navController.navigate(
                        Routes.reflection(mode = "optional", affirmation = false),
                    ) {
                        popUpTo(Routes.Alert) { inclusive = true }
                    }
                },
                onDialReturn = {
                    navController.navigate(Routes.CallOutcome) {
                        popUpTo(Routes.Alert) { inclusive = true }
                    }
                },
                onDismiss = {
                    alertBus.tryEmitDismissed()
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.CallOutcome) {
            CallOutcomeRoute(
                contactName = profile.emergencyContact.name,
                onReachedThem = {
                    alertBus.tryEmitDismissed()
                    navController.navigate(Routes.ReachedPraise) {
                        popUpTo(Routes.CallOutcome) { inclusive = true }
                    }
                },
                onDidNotAnswer = {
                    alertBus.tryEmitDismissed()
                    navController.navigate(
                        Routes.reflection(mode = "mandatory", affirmation = true),
                    ) {
                        popUpTo(Routes.CallOutcome) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ReachedPraise) {
            ReachedPraiseRoute(
                onDone = { goHomeClearingAlertStack() },
            )
        }
        composable(
            route = Routes.Reflection,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("affirmation") { type = NavType.StringType },
            ),
        ) { entry ->
            val modeArg = entry.arguments?.getString("mode") ?: "optional"
            val affirmation = entry.arguments?.getString("affirmation") == "1"
            val mode = if (modeArg == "mandatory") {
                ReflectionMode.MANDATORY
            } else {
                ReflectionMode.OPTIONAL
            }
            ReflectionRoute(
                mode = mode,
                showAffirmationFirst = affirmation,
                onFinished = {
                    alertBus.tryEmitDismissed()
                    goHomeClearingAlertStack()
                },
                onSkip = {
                    alertBus.tryEmitDismissed()
                    goHomeClearingAlertStack()
                },
            )
        }
    }
}
