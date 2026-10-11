package com.opesware.childtrace.parent.data

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.Policy
import com.opesware.childtrace.shared.model.Severity

/**
 * Backend-backed [ParentRepository]. The UI reads from an in-memory cache;
 * [refresh] pulls the latest alerts and activity from the backend. A ViewModel
 * calls [refresh] off the main thread, then recomposes.
 *
 * The per-app summary is derived from the activity stream, counting flagged
 * items per app — the same flag-only view the design shows (never a message
 * list).
 */
class BackendParentRepository(
    private val childId: String,
    private val displayName: String,
    private val band: AgeBand,
    private val client: BackendParentClient,
) : ParentRepository {

    @Volatile private var cachedAlerts: List<Alert> = emptyList()
    @Volatile private var cachedActivity: List<ActivityRecord> = emptyList()

    suspend fun refresh() {
        cachedAlerts = client.alerts(childId)
        cachedActivity = client.activity(childId)
    }

    override fun childName() = displayName
    override fun childPolicy(): Policy = Policy.defaultFor(band)
    override fun alerts(): List<Alert> = cachedAlerts.sortedByDescending { it.severity.rank }

    override fun activityByApp(): List<AppActivitySummary> =
        cachedActivity
            .filter { it.sourceApp != null }
            .groupBy { it.sourceApp!! }
            .map { (pkg, records) ->
                val flagged = records.filter { it.isFlagged }
                val top = flagged.mapNotNull { it.severity }.maxByOrNull { it.rank }
                AppActivitySummary(
                    appLabel = appLabel(pkg),
                    packageName = pkg,
                    flaggedCount = flagged.size,
                    topSeverity = top,
                    lastActivity = summaryLine(flagged.size, top),
                )
            }
            .sortedByDescending { it.flaggedCount }

    private fun summaryLine(flaggedCount: Int, top: Severity?): String =
        if (flaggedCount == 0) "No flags · monitored"
        else "$flaggedCount flagged · ${top?.name?.lowercase() ?: ""}".trim()

    private fun appLabel(pkg: String): String = when (pkg) {
        "com.whatsapp" -> "WhatsApp"
        "com.instagram.android" -> "Instagram"
        "org.telegram.messenger" -> "Telegram"
        "com.snapchat.android" -> "Snapchat"
        "com.zhiliaoapp.musically" -> "TikTok"
        else -> pkg
    }
}
