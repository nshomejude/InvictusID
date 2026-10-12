package com.opesware.childtrace.child.media

import com.opesware.childtrace.child.sync.BackendClient
import com.opesware.childtrace.shared.engine.MediaPolicy
import com.opesware.childtrace.shared.model.CapturedMedia
import com.opesware.childtrace.shared.model.MediaKind
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.Severity
import java.util.UUID

/**
 * The on-device gate for any media before it is sealed and uploaded. Every path
 * that would send a screenshot or image runs through here, and here it must pass
 * [MediaPolicy] — the same pure guard the backend re-runs. This is what keeps
 * capture flagged-only and screen-capture explicit/visible on the device itself,
 * not merely at the server.
 */
class MediaCapturer(
    private val childId: String,
    private val client: BackendClient,
    private val config: MediaPolicy.Config,
) {
    /**
     * Try to capture [bytes] as media. Returns true only if the policy allowed it
     * and the upload was attempted; false (with [onRefused]) if the policy refused
     * — in which case nothing leaves the device.
     */
    suspend fun capture(
        bytes: ByteArray,
        kind: MediaKind,
        trigger: MediaTrigger,
        mimeType: String,
        alertId: String? = null,
        linkedAlertSeverity: Severity? = null,
        onRefused: (String) -> Unit = {},
    ): Boolean {
        when (val decision = MediaPolicy.evaluate(kind, trigger, linkedAlertSeverity, config)) {
            is MediaPolicy.Decision.Refused -> {
                onRefused(decision.reason)
                return false
            }
            MediaPolicy.Decision.Allowed -> {
                val media = CapturedMedia(
                    id = UUID.randomUUID().toString(),
                    childId = childId,
                    kind = kind,
                    trigger = trigger,
                    alertId = alertId,
                    mimeType = mimeType,
                    capturedAtEpochMillis = System.currentTimeMillis(),
                )
                client.uploadMedia(media, bytes)
                return true
            }
        }
    }
}
