package com.alcolarm.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alcolarm.app.ui.SplashScreen
import com.alcolarm.feature.alert.AlertRoute
import com.alcolarm.feature.emergency.EmergencyRoute
import com.alcolarm.feature.location.HomeRoute
import com.alcolarm.feature.onboarding.OnboardingRoute
import com.alcolarm.feature.riskplaces.RiskPlacesRoute
import kotlinx.coroutines.delay

@Composable
fun AlcoLarmNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startViewModel: StartDestinationViewModel = hiltViewModel(),
) {

    NavHost(
        navController = navController,
        startDestination = Routes.Splash,
        modifier = modifier,
    ) {
        composable(Routes.Splash) {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(700)
                val complete = startViewModel.onboardingComplete.value
                val dest = if (complete) Routes.Home else Routes.Onboarding
                navController.navigate(dest) {
                    popUpTo(Routes.Splash) { inclusive = true }
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
                onSimulateAlert = {
                    navController.navigate(Routes.Alert)
                },
            )
        }
        composable(Routes.Alert) {
            AlertRoute(
                onDismiss = {
                    navController.popBackStack()
                },
            )
        }
    }
}
