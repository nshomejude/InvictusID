package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * The resolved rule set for a child, derived from their [AgeBand]. The parent
 * can tune the control surface fields; the [band] and its risk sensitivity are
 * set by the age bound to the consent record and are not freely editable.
 */
@Serializable
data class Policy(
    val band: AgeBand,
    /** Minimum severity that is surfaced to the parent for this band. */
    val minSurfacedSeverity: Severity,
    /** Package names explicitly allowed (whitelist for the RESTRICT band). */
    val allowedApps: Set<String> = emptySet(),
    /** Package names explicitly blocked. */
    val blockedApps: Set<String> = emptySet(),
    /** Daily screen-time cap in minutes, or null for no cap. */
    val dailyScreenTimeMinutes: Int? = null,
    /** Whether location sharing is enabled (always visible to the child). */
    val locationSharingEnabled: Boolean = false,
    /** Whether app installs require parent approval (younger bands). */
    val requireInstallApproval: Boolean = false,
) {
    companion object {
        /** The default policy for a band, before any parent customization. */
        fun defaultFor(band: AgeBand): Policy = when (band) {
            AgeBand.RESTRICT -> Policy(
                band = band,
                // Youngest: messaging mostly blocked, so the engine is a backstop;
                // anything it does catch is worth surfacing.
                minSurfacedSeverity = Severity.LOW,
                dailyScreenTimeMinutes = 60,
                locationSharingEnabled = true,
                requireInstallApproval = true,
            )
            AgeBand.FLAG_HIGH -> Policy(
                band = band,
                // Tweens: broad, sensitive flagging.
                minSurfacedSeverity = Severity.MEDIUM,
                dailyScreenTimeMinutes = 120,
                locationSharingEnabled = true,
                requireInstallApproval = true,
            )
            AgeBand.FLAG_NARROWED -> Policy(
                band = band,
                // Teens: only real danger reaches the parent.
                minSurfacedSeverity = Severity.HIGH,
                locationSharingEnabled = false,
                requireInstallApproval = false,
            )
        }
    }
}
