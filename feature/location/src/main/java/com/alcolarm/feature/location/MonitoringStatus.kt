package com.alcolarm.feature.location

import androidx.annotation.StringRes
import com.alcolarm.core.model.RiskPlaceId

enum class MonitoringUiState {
    PERMISSION_NEEDED,
    NO_DETECTABLE_RISKS,
    WATCHING,
    NEAR_RISK,
    CHECK_ERROR,
    SNOOZED,
}

enum class WatchModeUi {
    OFF,
    FOREGROUND_ONLY,
    BACKGROUND,
}

/** Localized monitoring status — resolve with stringResource / getString. */
enum class MonitoringStatusKey {
    PERMISSION_NEEDED,
    STARTING,
    NO_DETECTABLE_RISKS,
    GETTING_LOCATION,
    WATCHING,
    NEAR_CONFIRMING,
    CHECK_FAILED,
    BACKGROUND_LOCATION_NEEDED,
    SNOOZED,
    ;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            PERMISSION_NEEDED -> R.string.status_permission_needed
            STARTING -> R.string.status_starting
            NO_DETECTABLE_RISKS -> R.string.status_no_detectable_risks
            GETTING_LOCATION -> R.string.status_getting_location
            WATCHING -> R.string.status_watching
            NEAR_CONFIRMING -> R.string.status_near_confirming
            CHECK_FAILED -> R.string.status_check_failed
            BACKGROUND_LOCATION_NEEDED -> R.string.status_background_location_needed
            SNOOZED -> R.string.status_paused_until_fallback
        }
}

data class HomeMonitoringUi(
    val permissionGranted: Boolean = false,
    val monitoringActive: Boolean = false,
    val watchMode: WatchModeUi = WatchModeUi.OFF,
    val needsBackgroundLocation: Boolean = false,
    val uiState: MonitoringUiState = MonitoringUiState.PERMISSION_NEEDED,
    val statusKey: MonitoringStatusKey = MonitoringStatusKey.PERMISSION_NEEDED,
    val lastMatchedPlaceName: String? = null,
    val lastMatchedRisk: RiskPlaceId? = null,
    /** Non-zero while user pause / snooze window is active. */
    val snoozeUntilEpochMs: Long = 0L,
)
