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
}

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
