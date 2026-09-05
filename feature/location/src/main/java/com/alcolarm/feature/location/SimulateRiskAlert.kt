package com.alcolarm.feature.location

/**
 * Explicit simulate path for MVP testing (debug + release).
 * Home "Simulate risk alert" emits via [RiskAlertBus]; this action string
 * remains for any external/debug triggers.
 */
object SimulateRiskAlert {
    const val ACTION = "com.alcolarm.app.SIMULATE_RISK_ALERT"
}
