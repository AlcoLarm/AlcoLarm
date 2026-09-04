package com.alcolarm.core.model

/**
 * Persisted onboarding + prefs. No location history by design.
 */
data class UserProfile(
    val onboardingComplete: Boolean = false,
    val quitReasons: Set<QuitReasonId> = emptySet(),
    val healthNotes: String = "",
    val familyNotes: String = "",
    val familyPhotoUris: List<String> = emptyList(),
    val riskPlaces: Set<RiskPlaceId> = emptySet(),
    val emergencyContact: EmergencyContact = EmergencyContact(),
)
