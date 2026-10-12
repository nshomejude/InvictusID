package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * A crisis / support resource surfaced alongside a serious flag — most
 * importantly self-harm. The parent always sees these; an age-appropriate
 * subset is shown to the child on their device. Resources are region-aware
 * (Cameroon first) with an international fallback.
 */
@Serializable
data class SafetyResource(
    val title: String,
    val description: String,
    /** Phone number or short code, shown as selectable text. */
    val contact: String,
    val region: String,
    /** True if appropriate to show directly to the child. */
    val childFacing: Boolean,
)

object SafetyResources {

    /**
     * Resources for a flagged [category] in a [region] (ISO country code).
     * Returns an empty list for categories without a crisis pathway.
     * Figures here are placeholders to be confirmed with local services before
     * launch — do not treat the numbers as verified.
     */
    fun forCategory(category: RiskCategory, region: String = "CM"): List<SafetyResource> =
        when (category) {
            RiskCategory.SELF_HARM -> selfHarm(region)
            RiskCategory.GROOMING, RiskCategory.SEXUAL_CONTENT -> exploitation(region)
            else -> emptyList()
        }

    private fun selfHarm(region: String): List<SafetyResource> = buildList {
        if (region == "CM") {
            add(
                SafetyResource(
                    title = "Cameroon mental-health support",
                    description = "Talk to someone now. (Confirm the current national line before launch.)",
                    contact = "TBD-CM-LINE",
                    region = "CM",
                    childFacing = true,
                )
            )
        }
        // International fallback (Befrienders Worldwide directory).
        add(
            SafetyResource(
                title = "Befrienders Worldwide",
                description = "Find a helpline near you.",
                contact = "https://befrienders.org",
                region = "INTL",
                childFacing = true,
            )
        )
    }

    private fun exploitation(region: String): List<SafetyResource> = listOf(
        SafetyResource(
            title = "Reporting online child exploitation",
            description = "Guidance for parents on reporting grooming or sexual content to authorities.",
            contact = "https://report.cybertip.org",
            region = "INTL",
            childFacing = false,
        )
    )
}
