package com.opesware.childtrace.parent.data

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.AlertStatus
import com.opesware.childtrace.shared.model.Policy
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

/**
 * Supplies the Parent app with data. This mock drives the UI with illustrative
 * examples until the backend client is wired; the real implementation pulls the
 * ranked alert feed and activity from the Opesware backend over TLS.
 */
interface ParentRepository {
    fun childName(): String
    fun childPolicy(): Policy
    fun alerts(): List<Alert>
    fun activityByApp(): List<AppActivitySummary>
}

/** One row of the "Activity by app" screen: an app and its flag tally. */
data class AppActivitySummary(
    val appLabel: String,
    val packageName: String,
    val flaggedCount: Int,
    val topSeverity: Severity?,
    val lastActivity: String,
)

class MockParentRepository : ParentRepository {
    override fun childName() = "Amara · 11"

    override fun childPolicy() = Policy.defaultFor(AgeBand.FLAG_HIGH)

    override fun alerts(): List<Alert> = listOf(
        Alert(
            id = "a1", childId = "c1",
            category = RiskCategory.GROOMING, severity = Severity.CRITICAL,
            createdAtEpochMillis = now - 2 * 60_000,
            sourceApp = "com.instagram.android",
            contextSnippet = "you're so mature… let's talk on snapchat, don't tell your mom — it's our secret",
            reason = "Unknown contact combined platform-switching, flattery, and a request for secrecy.",
            status = AlertStatus.NEW,
        ),
        Alert(
            id = "a2", childId = "c1",
            category = RiskCategory.SEXUAL_CONTENT, severity = Severity.HIGH,
            createdAtEpochMillis = now - 60 * 60_000,
            sourceApp = "com.whatsapp",
            contextSnippet = "Request for images from a first-time contact.",
            reason = "Sexual content directed at the child.",
            status = AlertStatus.NEW,
        ),
        Alert(
            id = "a3", childId = "c1",
            category = RiskCategory.BULLYING, severity = Severity.MEDIUM,
            createdAtEpochMillis = now - 20 * 60 * 60_000,
            sourceApp = "com.zhiliaoapp.musically",
            contextSnippet = "Repeated hostile messages in a group chat.",
            reason = "Language associated with bullying or harassment.",
            status = AlertStatus.REVIEWED,
        ),
    )

    override fun activityByApp(): List<AppActivitySummary> = listOf(
        AppActivitySummary("Instagram", "com.instagram.android", 1, Severity.CRITICAL, "1 critical · 2m ago"),
        AppActivitySummary("WhatsApp", "com.whatsapp", 1, Severity.HIGH, "1 high · 1h ago"),
        AppActivitySummary("TikTok", "com.zhiliaoapp.musically", 1, Severity.MEDIUM, "1 medium · today"),
        AppActivitySummary("Snapchat", "com.snapchat.android", 0, null, "No flags · monitored"),
        AppActivitySummary("Telegram", "org.telegram.messenger", 0, null, "No flags · monitored"),
    )

    private val now get() = System.currentTimeMillis()
}

/** Example of a non-flagged activity record, for the activity detail view. */
val sampleActivity = ActivityRecord(
    id = "act1", childId = "c1", type = ActivityType.MESSAGE,
    createdAtEpochMillis = System.currentTimeMillis(),
    sourceApp = "com.whatsapp", content = "see you at practice!", counterparty = "Team chat",
)
