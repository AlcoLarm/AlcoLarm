package com.alcolarm.app.navigation

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val RiskPlaces = "risk_places"
    const val Emergency = "emergency"
    const val Home = "home"
    const val Alert = "alert"
    const val CallOutcome = "call_outcome"
    const val ReachedPraise = "reached_praise"
    const val Reflection = "reflection/{mode}/{affirmation}"

    fun reflection(mode: String, affirmation: Boolean): String =
        "reflection/$mode/${if (affirmation) "1" else "0"}"
}
