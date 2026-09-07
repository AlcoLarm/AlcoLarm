package com.alcolarm.core.model

import androidx.annotation.StringRes

/**
 * Why the person is quitting — shown later on the alert screen for motivation.
 */
enum class QuitReasonId {
    HEALTH,
    FAMILY,
    MONEY,
    WORK,
    SELF_RESPECT,
    OTHER,
    ;

    /** User-facing label string resource for chips, home summary, and alert. */
    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            HEALTH -> R.string.quit_reason_health
            FAMILY -> R.string.quit_reason_family
            MONEY -> R.string.quit_reason_money
            WORK -> R.string.quit_reason_work
            SELF_RESPECT -> R.string.quit_reason_self_respect
            OTHER -> R.string.quit_reason_other
        }

    /** English fallback for non-UI / logging. Prefer [labelRes] in Compose. */
    @Deprecated("Use labelRes with stringResource / getString", ReplaceWith("labelRes"))
    val displayLabel: String
        get() = when (this) {
            HEALTH -> "Health"
            FAMILY -> "Loved ones"
            MONEY -> "Money"
            WORK -> "Work"
            SELF_RESPECT -> "Self-respect"
            OTHER -> "Something else"
        }
}

/** Alias kept for call sites; prefer Compose stringResource(id.labelRes). */
@Deprecated("Use labelRes with stringResource", ReplaceWith("labelRes"))
fun QuitReasonId.friendly(): String = displayLabel

data class QuitReasonSelection(
    val id: QuitReasonId,
    val label: String,
    val selected: Boolean = false,
)

data class HealthNotes(
    val text: String = "",
)

data class FamilyNotes(
    val text: String = "",
    /** Local URI strings for placeholder loved-ones photos (MVP stub). */
    val photoUris: List<String> = emptyList(),
)
