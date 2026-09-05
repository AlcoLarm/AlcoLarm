package com.alcolarm.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.alcolarm.core.model.EmergencyContact
import com.alcolarm.core.model.QuitReasonId
import com.alcolarm.core.model.RiskPlaceId
import com.alcolarm.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed prefs. Intentionally stores no location history —
 * only live location is used at runtime (see :feature:location).
 * Family photo entries are relative paths under filesDir (see [FamilyPhotoStore]),
 * not grant-dependent content URIs.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val profile: Flow<UserProfile> = dataStore.data.map { prefs ->
        UserProfile(
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
            quitReasons = (prefs[Keys.QUIT_REASONS] ?: emptySet())
                .mapNotNull { runCatching { QuitReasonId.valueOf(it) }.getOrNull() }
                .toSet(),
            healthNotes = prefs[Keys.HEALTH_NOTES] ?: "",
            familyNotes = prefs[Keys.FAMILY_NOTES] ?: "",
            familyPhotoUris = (prefs[Keys.FAMILY_PHOTOS] ?: emptySet()).toList(),
            riskPlaces = (prefs[Keys.RISK_PLACES] ?: emptySet())
                .mapNotNull { runCatching { RiskPlaceId.valueOf(it) }.getOrNull() }
                .toSet(),
            emergencyContact = EmergencyContact(
                name = prefs[Keys.EMERGENCY_NAME] ?: "",
                phoneNumber = prefs[Keys.EMERGENCY_PHONE] ?: "",
            ),
        )
    }

    suspend fun setQuitReasons(
        reasons: Set<QuitReasonId>,
        healthNotes: String,
        familyNotes: String,
        familyPhotoUris: List<String>,
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.QUIT_REASONS] = reasons.map { it.name }.toSet()
            prefs[Keys.HEALTH_NOTES] = healthNotes
            prefs[Keys.FAMILY_NOTES] = familyNotes
            prefs[Keys.FAMILY_PHOTOS] = familyPhotoUris.toSet()
        }
    }

    suspend fun setRiskPlaces(places: Set<RiskPlaceId>) {
        dataStore.edit { prefs ->
            prefs[Keys.RISK_PLACES] = places.map { it.name }.toSet()
        }
    }

    suspend fun setEmergencyContact(contact: EmergencyContact) {
        dataStore.edit { prefs ->
            prefs[Keys.EMERGENCY_NAME] = contact.name
            prefs[Keys.EMERGENCY_PHONE] = contact.phoneNumber
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun markOnboardingComplete() {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = true
        }
    }

    /** User wants continuous watch via foreground service when background location is granted. */
    val backgroundWatchEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.BACKGROUND_WATCH_ENABLED] ?: true
    }

    suspend fun setBackgroundWatchEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BACKGROUND_WATCH_ENABLED] = enabled
        }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val QUIT_REASONS = stringSetPreferencesKey("quit_reasons")
        val HEALTH_NOTES = stringPreferencesKey("health_notes")
        val FAMILY_NOTES = stringPreferencesKey("family_notes")
        val FAMILY_PHOTOS = stringSetPreferencesKey("family_photos")
        val RISK_PLACES = stringSetPreferencesKey("risk_places")
        val EMERGENCY_NAME = stringPreferencesKey("emergency_name")
        val EMERGENCY_PHONE = stringPreferencesKey("emergency_phone")
        val BACKGROUND_WATCH_ENABLED = booleanPreferencesKey("background_watch_enabled")
    }
}
