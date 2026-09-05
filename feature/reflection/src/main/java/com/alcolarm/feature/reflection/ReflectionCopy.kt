package com.alcolarm.feature.reflection

import com.alcolarm.core.model.QuitReasonId
import com.alcolarm.core.model.UserProfile
import com.alcolarm.core.model.friendly

/**
 * Short reflection cards — why they quit + honest look at giving in.
 * Respectful, non-shameful, personalized from profile when possible.
 */
data class ReflectionCard(
    val title: String,
    val body: String,
)

object ReflectionCopy {
    fun buildCards(profile: UserProfile): List<ReflectionCard> {
        val reasons = profile.quitReasons
        val reasonLine = if (reasons.isEmpty()) {
            "the life you’re building"
        } else {
            reasons.joinToString(", ") { it.friendly().lowercase() }
        }

        val whyYouQuit = buildString {
            append("You chose this path for ")
            append(reasonLine)
            append(".")
            val note = listOf(profile.familyNotes, profile.healthNotes)
                .map { it.trim() }
                .firstOrNull { it.isNotEmpty() }
            if (note != null) {
                append("\n\nYour words: “")
                append(note)
                append("”")
            }
        }

        val cards = mutableListOf(
            ReflectionCard(
                title = "Why you stopped",
                body = whyYouQuit,
            ),
            ReflectionCard(
                title = "Who you’re protecting",
                body = lovedOnesBody(profile),
            ),
            ReflectionCard(
                title = "If the urge wins tonight",
                body = "One drink rarely stays one. Tomorrow often brings fog, guilt, " +
                    "and the same climb again — not because you’re weak, " +
                    "but because alcohol is designed that way.",
            ),
            ReflectionCard(
                title = "What you keep by walking away",
                body = keepByWalkingAway(reasons),
            ),
            ReflectionCard(
                title = "This urge will pass",
                body = "Urges crest and fall. You’ve already paused — that’s the hardest step. " +
                    "Leave this place, call someone, or breathe for two minutes. You can ride this out.",
            ),
        )
        return cards
    }

    private fun lovedOnesBody(profile: UserProfile): String {
        val note = profile.familyNotes.trim()
        return when {
            note.isNotEmpty() ->
                "Hold onto this: “$note”\n\n" +
                    "They don’t need perfection — they need you present and clear."
            QuitReasonId.FAMILY in profile.quitReasons ->
                "Your loved ones matter enough that you put them on this list. " +
                    "Staying sober tonight is a gift you give them — and yourself."
            else ->
                "The people who care about you want you safe and free. " +
                    "You don’t have to face this alone."
        }
    }

    private fun keepByWalkingAway(reasons: Set<QuitReasonId>): String {
        val bits = mutableListOf<String>()
        if (QuitReasonId.HEALTH in reasons) bits += "clearer health and energy"
        if (QuitReasonId.FAMILY in reasons) bits += "trust with loved ones"
        if (QuitReasonId.MONEY in reasons) bits += "money that stays yours"
        if (QuitReasonId.WORK in reasons) bits += "steady focus at work"
        if (QuitReasonId.SELF_RESPECT in reasons) bits += "self-respect you can feel"
        if (bits.isEmpty()) {
            bits += "momentum"
            bits += "a calmer morning"
            bits += "proof you can choose yourself"
        }
        return "Walking away keeps " + bits.joinToString(", ") +
            ". Small choices stack into the life you wanted when you quit."
    }

    const val AFFIRMATION_TITLE = "You did the right thing"
    const val AFFIRMATION_BODY =
        "Reaching out was brave — even if they couldn’t pick up. " +
            "Don’t worry that they weren’t available right now. " +
            "You made the right decision stopping and trying. " +
            "Let’s take a few quiet moments to ground yourself."

    const val REACHED_TITLE = "You reached them"
    const val REACHED_BODY =
        "That’s exactly what this moment called for. " +
            "Take a breath — you’re not alone in this. Head home when you’re ready."
}
