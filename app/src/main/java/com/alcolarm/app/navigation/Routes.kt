package com.alcolarm.app.navigation

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val RiskPlaces = "risk_places"
    const val Emergency = "emergency"
    const val Home = "home"
    const val Settings = "settings"
    const val EditReasons = "edit_reasons"
    const val EditRiskPlaces = "edit_risk_places"
    const val EditEmergency = "edit_emergency"
    const val Alert = "alert"
    const val CallOutcome = "call_outcome"
    const val ReachedPraise = "reached_praise"
    const val Reflection = "reflection/{mode}/{affirmation}"

    fun reflection(mode: String, affirmation: Boolean): String =
        "reflection/$mode/${if (affirmation) "1" else "0"}"
}
