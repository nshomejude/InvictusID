package com.opesware.childtrace.parent.data

import com.opesware.childtrace.shared.crypto.EnvelopeCrypto
import com.opesware.childtrace.shared.crypto.SealedEnvelope
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.Policy
import com.opesware.childtrace.shared.model.Severity
import kotlinx.serialization.json.Json

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
    private val keystore: ParentKeystore,
) : ParentRepository {

    @Volatile private var cachedAlerts: List<Alert> = emptyList()
    @Volatile private var cachedActivity: List<ActivityRecord> = emptyList()

    /** Registers this parent's current public key for the child. Call at pairing and after rotate(). */
    suspend fun registerKey() =
        client.registerParentKey(childId, keystore.currentKeyId(), keystore.currentPublicKeySpki())

    suspend fun refresh() {
        cachedAlerts = client.alerts(childId)
        cachedActivity = client.activity(childId).map { it.copy(content = openContent(it.content)) }
    }

    /**
     * Opens envelope-sealed content by routing to the private key named in the
     * envelope's keyId. Content sealed to a key no longer on this device (lost
     * in a rotation) can't be opened — shown as a placeholder rather than
     * failing the whole refresh. Plaintext (e.g. mock data) is left as-is.
     */
    private fun openContent(stored: String): String {
        val envelope = runCatching { Json.decodeFromString(SealedEnvelope.serializer(), stored) }
            .getOrNull() ?: return stored
        val privateKey = keystore.privateKeyOrNull(envelope.keyId)
            ?: return "[encrypted with a previous key — unavailable on this device]"
        return runCatching { EnvelopeCrypto.openToString(envelope, privateKey) }
            .getOrDefault("[could not decrypt]")
    }

    override suspend fun mediaList(): List<MediaItemUi> =
        client.mediaRefs(childId).map { ref ->
            val m = ref.media
            MediaItemUi(
                id = m.id,
                kindLabel = m.kind.name.lowercase().replaceFirstChar { it.uppercase() },
                reason = when (m.trigger) {
                    MediaTrigger.FLAGGED_ALERT -> "Captured with a safety alert"
                    MediaTrigger.PARENT_REQUESTED_SCREEN -> "Screenshot you requested"
                },
                mimeType = m.mimeType,
                capturedAtEpochMillis = m.capturedAtEpochMillis,
                // We can open it only if the private key it was sealed to is still here.
                openableOnThisDevice = keystore.privateKeyOrNull(ref.keyId) != null,
            )
        }

    override suspend fun openMedia(mediaId: String): ByteArray? {
        val sealed = runCatching { client.media(childId, mediaId) }.getOrNull() ?: return null
        val privateKey = keystore.privateKeyOrNull(sealed.envelope.keyId) ?: return null
        return runCatching { EnvelopeCrypto.open(sealed.envelope, privateKey) }.getOrNull()
    }

    override fun childName() = displayName
    override fun childPolicy(): Policy = Policy.defaultFor(band)
    override fun alerts(): List<Alert> = cachedAlerts.sortedByDescending { it.severity.rank }

    override fun activity(): List<ActivityRecord> = cachedActivity
        .sortedWith(compareByDescending<ActivityRecord> { it.isFlagged }.thenByDescending { it.createdAtEpochMillis })

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
