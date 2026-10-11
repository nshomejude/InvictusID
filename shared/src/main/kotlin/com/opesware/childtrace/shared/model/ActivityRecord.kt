package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * A unit of captured device activity synced to the parent under the
 * comprehensive-monitoring model.
 *
 * This is distinct from [Alert]: an Alert is a ranked danger signal produced by
 * the risk engine; an ActivityRecord is the broader monitored activity a vetted
 * parent of a minor may review. Both are governed by the same hard rules — the
 * child is told (always visible), it is the parent's own minor child only, and
 * every record is encrypted, access-logged, and retention-bounded.
 *
 * [riskCategory]/[severity] are set when the on-device engine also flagged this
 * record, so the parent UI can rank danger to the top rather than leaving a
 * parent to scroll.
 */
@Serializable
data class ActivityRecord(
    val id: String,
    val childId: String,
    val type: ActivityType,
    val createdAtEpochMillis: Long,
    /** Source app package where relevant (messaging/browsing), else null. */
    val sourceApp: String? = null,
    /** The captured content (message text, URL, app name, etc.). */
    val content: String,
    /** Best-effort counterparty label for a message, if known. */
    val counterparty: String? = null,
    /** Set when the risk engine flagged this activity; null for ordinary activity. */
    val riskCategory: RiskCategory? = null,
    val severity: Severity? = null,
) {
    /** True when this record also carries a danger flag and should rank first. */
    val isFlagged: Boolean get() = riskCategory != null
}

@Serializable
enum class ActivityType {
    MESSAGE,
    BROWSING,
    APP_USAGE,
    APP_INSTALL,
    LOCATION,
    SCREEN_TEXT,
}
