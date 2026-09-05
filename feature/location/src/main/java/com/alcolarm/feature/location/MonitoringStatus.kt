package com.alcolarm.feature.location

import com.alcolarm.core.model.RiskPlaceId

enum class MonitoringUiState {
    PERMISSION_NEEDED,
    NO_DETECTABLE_RISKS,
    WATCHING,
    NEAR_RISK,
    CHECK_ERROR,
}

data class HomeMonitoringUi(
    val permissionGranted: Boolean = false,
    val monitoringActive: Boolean = false,
    val uiState: MonitoringUiState = MonitoringUiState.PERMISSION_NEEDED,
    val statusMessage: String = "Permission needed",
    val lastMatchedPlaceName: String? = null,
    val lastMatchedRisk: RiskPlaceId? = null,
)
