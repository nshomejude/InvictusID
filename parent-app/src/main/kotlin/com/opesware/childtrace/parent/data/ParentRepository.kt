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
    /** The full decrypted activity stream, danger ranked first — comprehensive visible monitoring. */
    fun activity(): List<ActivityRecord>

    /**
     * Captured media (screenshots/images tied to an alert, or explicit screen
     * captures), newest first. Metadata only — the bytes are fetched and
     * decrypted on demand by [openMedia]. Defaults to empty so the mock and any
     * non-media repository need not implement it.
     */
    suspend fun mediaList(): List<MediaItemUi> = emptyList()

    /**
     * Fetches one media item and decrypts it **locally** with the parent's
     * private key. Returns the raw bytes, or null if it can't be decrypted on
     * this device (e.g. sealed to a rotated-out key). There is no server-side
     * decryption and no web-admin view — the plaintext exists only here.
     */
    suspend fun openMedia(mediaId: String): ByteArray? = null
}

/** One row of the parent's media gallery. */
data class MediaItemUi(
    val id: String,
    val kindLabel: String,
    val reason: String,
    val mimeType: String,
    val capturedAtEpochMillis: Long,
    /** False when this item was sealed to a key no longer on this device. */
    val openableOnThisDevice: Boolean,
)

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

    override fun activity(): List<ActivityRecord> = listOf(
        ActivityRecord("r1", "c1", ActivityType.MESSAGE, now - 2 * 60_000, "com.instagram.android",
            "you're so mature… let's talk on snapchat", "unknown", RiskCategory.GROOMING, Severity.CRITICAL),
        ActivityRecord("r2", "c1", ActivityType.MESSAGE, now - 60 * 60_000, "com.whatsapp",
            "see you at practice!", "Team chat"),
        ActivityRecord("r3", "c1", ActivityType.BROWSING, now - 90 * 60_000, "com.android.chrome",
            "https://www.khanacademy.org/math", null),
        ActivityRecord("r4", "c1", ActivityType.APP_USAGE, now - 3 * 60 * 60_000, "com.zhiliaoapp.musically",
            "TikTok · 42 min", null),
        ActivityRecord("r5", "c1", ActivityType.LOCATION, now - 4 * 60 * 60_000, null,
            "Near Douala, Akwa", null),
    )

    private val now get() = System.currentTimeMillis()
}

/** Example of a non-flagged activity record, for the activity detail view. */
val sampleActivity = ActivityRecord(
    id = "act1", childId = "c1", type = ActivityType.MESSAGE,
    createdAtEpochMillis = System.currentTimeMillis(),
    sourceApp = "com.whatsapp", content = "see you at practice!", counterparty = "Team chat",
)
