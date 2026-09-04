package com.alcolarm.core.model

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

    /** User-facing label for chips, home summary, and alert. */
    val displayLabel: String
        get() = when (this) {
            HEALTH -> "Health"
            FAMILY -> "Family"
            MONEY -> "Money"
            WORK -> "Work"
            SELF_RESPECT -> "Self-respect"
            OTHER -> "Something else"
        }
}

/** Alias for call sites that prefer extension-style naming. */
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
    /** Local URI strings for placeholder family photos (MVP stub). */
    val photoUris: List<String> = emptyList(),
)
