package com.opesware.childtrace.shared.model

/**
 * A single unit of text captured on the child's device, before any analysis.
 *
 * This object never leaves the device. The risk engine scores it on-device and,
 * unless it is flagged, it is discarded immediately — normal messages are never
 * transmitted or stored (data minimization, from the spec).
 */
data class CapturedMessage(
    /** Monotonic capture time (epoch millis). */
    val capturedAtEpochMillis: Long,
    /** The package name of the app the text came from, e.g. "com.whatsapp". */
    val sourceApp: String,
    /** Which capture feed produced this. */
    val source: CaptureSource,
    /** The message text as displayed on-device. */
    val text: String,
    /** Best-effort sender label from the notification/screen, if known. */
    val sender: String? = null,
    /**
     * True when the engine has reason to believe the sender is an unknown /
     * first-contact party (new number, not in contacts). Drives grooming and
     * stranger-escalation heuristics. The capture layer sets this best-effort.
     */
    val senderIsUnknownContact: Boolean = false,
)

/** Which on-device feed a [CapturedMessage] came from. */
enum class CaptureSource {
    /** NotificationListenerService — primary feed (sender + preview). */
    NOTIFICATION,

    /** AccessibilityService — gap-filler feed (on-screen text). */
    ACCESSIBILITY,
}
