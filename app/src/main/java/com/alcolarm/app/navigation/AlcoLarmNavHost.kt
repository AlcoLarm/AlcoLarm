package com.alcolarm.app.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alcolarm.app.BuildConfig
import com.alcolarm.app.ui.SplashScreen
import com.alcolarm.feature.alert.AlertRoute
import com.alcolarm.feature.emergency.EmergencyRoute
import com.alcolarm.feature.location.HomeRoute
import com.alcolarm.feature.location.RiskAlertBus
import com.alcolarm.feature.onboarding.OnboardingRoute
import com.alcolarm.feature.riskplaces.RiskPlacesRoute
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

@Composable
fun AlcoLarmNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startViewModel: StartDestinationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val alertBus = EntryPointAccessors.fromApplication(
        context.applicationContext,
        RiskAlertBusEntryPoint::class.java,
    ).riskAlertBus()

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
                showSimulateAlert = BuildConfig.DEBUG,
            )
        }
        composable(Routes.Alert) {
            AlertRoute(
                onDismiss = {
                    alertBus.tryEmitDismissed()
                    navController.popBackStack()
                },
            )
        }
    }
}
