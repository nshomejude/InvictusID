package com.opesware.childtrace.shared.model

/**
 * The danger categories the on-device risk engine screens for, from the spec.
 *
 * [surfacesInNarrowedBand] marks the categories that still reach a 13–15 parent:
 * for teens, only genuine danger (grooming, sexual coercion, self-harm, violent
 * threat) is surfaced — bullying and generic stranger chatter are analyzed but
 * not escalated, to keep the product from drifting toward read-everything
 * surveillance of an older child.
 */
enum class RiskCategory(
    val label: String,
    val surfacesInNarrowedBand: Boolean,
) {
    GROOMING("Grooming", surfacesInNarrowedBand = true),
    SEXUAL_CONTENT("Sexual content", surfacesInNarrowedBand = true),
    BULLYING("Bullying & harassment", surfacesInNarrowedBand = false),
    SELF_HARM("Self-harm & suicidal ideation", surfacesInNarrowedBand = true),
    VIOLENCE_THREAT("Violence & threats", surfacesInNarrowedBand = true),
    STRANGER_ESCALATION("Stranger-contact escalation", surfacesInNarrowedBand = false),
}
