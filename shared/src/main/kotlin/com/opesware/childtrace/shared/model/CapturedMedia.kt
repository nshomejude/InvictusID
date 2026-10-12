package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * Visual evidence captured on the child's device: a screenshot or an image/video
 * the on-device engine has tied to a danger flag. It exists to let a parent *see*
 * the context of a real alert, not to mirror the device.
 *
 * Two things keep this on the right side of the stalkerware line, and both are
 * enforced by [MediaPolicy], never left to the capture code:
 *
 * 1. **Never bulk.** Media is only ever captured when it is justified — attached
 *    to an alert the risk engine already raised ([MediaTrigger.FLAGGED_ALERT]),
 *    or produced by an explicit, visible screen-capture the parent triggered
 *    ([MediaTrigger.PARENT_REQUESTED_SCREEN]). The camera roll and the full
 *    message inbox are never swept.
 * 2. **Never covert.** Screen capture runs through the platform MediaProjection
 *    API, whose system recording indicator stays visible and is never
 *    suppressed (CLAUDE.md invariant 1). The child-app disclosure states that
 *    screen capture is part of the monitoring.
 *
 * Like every other synced item, the bytes are envelope-sealed to the parent's
 * key on-device before they leave the phone (invariant 3). This object only
 * carries metadata plus the sealed reference; the plaintext never appears here.
 */
@Serializable
data class CapturedMedia(
    val id: String,
    val childId: String,
    val kind: MediaKind,
    val trigger: MediaTrigger,
    /** The alert this media evidences, when [trigger] is [MediaTrigger.FLAGGED_ALERT]. */
    val alertId: String? = null,
    /** MIME type of the sealed bytes, e.g. "image/png", "image/jpeg", "video/mp4". */
    val mimeType: String,
    val capturedAtEpochMillis: Long,
)

/** What a piece of [CapturedMedia] is. */
@Serializable
enum class MediaKind {
    /** A screenshot of the child's screen (MediaProjection). */
    SCREENSHOT,

    /** A still image the engine tied to a flag (e.g. an image shown in a flagged chat). */
    IMAGE,

    /** A short video clip the engine tied to a flag. */
    VIDEO,
}

/**
 * Why a piece of media was captured. This is the justification [MediaPolicy]
 * checks; there is deliberately no "all media" or "everything" value, because
 * bulk collection is exactly what the policy refuses.
 */
@Serializable
enum class MediaTrigger {
    /**
     * Captured because the on-device engine raised an alert and this media is
     * the visual context of that alert. Requires a linked alert at or above the
     * policy's severity floor.
     */
    FLAGGED_ALERT,

    /**
     * A screenshot the parent explicitly asked for, taken through the visible
     * MediaProjection capture (system indicator shown). One shot per request —
     * not a continuous feed.
     */
    PARENT_REQUESTED_SCREEN,
}
