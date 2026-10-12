package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.CapturedMedia
import com.opesware.childtrace.shared.model.MediaKind
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.Severity

/**
 * The guard that keeps media capture defensible. It is the media counterpart of
 * [Provisioning]: a pure, testable decision that both the child's capture code
 * and the backend ingest run, so neither can be talked into bulk or covert
 * collection by a bug or a future "feature".
 *
 * The product rule it encodes:
 *
 * - **Flagged media** ([MediaTrigger.FLAGGED_ALERT]) is allowed only when it is
 *   tied to an alert the risk engine already raised, and only when that alert is
 *   at or above [severityFloor]. No alert, or a below-floor alert, is refused —
 *   this is what stops the camera roll and the full message inbox from being
 *   swept up.
 * - **Explicit screen capture** ([MediaTrigger.PARENT_REQUESTED_SCREEN]) is a
 *   screenshot the parent deliberately triggered, taken through the visible
 *   MediaProjection path. It is allowed only when [allowScreenCapture] is on
 *   (the counsel-gated feature flag, invariant 5/7) and only for a
 *   [MediaKind.SCREENSHOT]. The system recording indicator is shown by the
 *   platform and is never suppressed; this guard does not grant covertness and
 *   nothing downstream may add it.
 *
 * There is intentionally no path that allows unsolicited, untriggered media.
 */
object MediaPolicy {

    /**
     * @param allowScreenCapture whether explicit parent-triggered screen capture
     *   is enabled for this deployment. Stands in for Cameroon counsel sign-off
     *   (CLAUDE.md invariant 7); refused by default, exactly like the senior-teen
     *   band. Flagged-media capture does not depend on this flag.
     * @param severityFloor the minimum alert severity whose context may be
     *   captured as media. Defaults to [Severity.HIGH] so only genuinely
     *   dangerous alerts ever pull in a screenshot/image.
     */
    data class Config(
        val allowScreenCapture: Boolean = false,
        val severityFloor: Severity = Severity.HIGH,
    )

    sealed interface Decision {
        object Allowed : Decision
        data class Refused(val reason: String) : Decision
    }

    /**
     * Decide whether a capture is permitted.
     *
     * @param kind what is being captured.
     * @param trigger the justification for capturing it.
     * @param linkedAlertSeverity the severity of the alert this media evidences,
     *   or null if there is no linked alert. Required (and must meet the floor)
     *   for [MediaTrigger.FLAGGED_ALERT].
     */
    fun evaluate(
        kind: MediaKind,
        trigger: MediaTrigger,
        linkedAlertSeverity: Severity?,
        config: Config = Config(),
    ): Decision = when (trigger) {
        MediaTrigger.FLAGGED_ALERT -> {
            when {
                linkedAlertSeverity == null ->
                    Decision.Refused("Media may only be captured as the context of a raised alert; no alert is linked.")
                linkedAlertSeverity.rank < config.severityFloor.rank ->
                    Decision.Refused(
                        "Alert severity ${linkedAlertSeverity.name} is below the capture floor " +
                            "${config.severityFloor.name}; no media is captured."
                    )
                else -> Decision.Allowed
            }
        }

        MediaTrigger.PARENT_REQUESTED_SCREEN -> {
            when {
                !config.allowScreenCapture ->
                    Decision.Refused("Explicit screen capture is disabled (requires counsel sign-off; CHILDTRACE_ALLOW_SCREEN_CAPTURE).")
                kind != MediaKind.SCREENSHOT ->
                    Decision.Refused("Parent-requested screen capture produces a screenshot only, not ${kind.name}.")
                else -> Decision.Allowed
            }
        }
    }

    /** Convenience overload: evaluate a fully-formed [CapturedMedia] against its linked alert severity. */
    fun evaluate(media: CapturedMedia, linkedAlertSeverity: Severity?, config: Config = Config()): Decision =
        evaluate(media.kind, media.trigger, linkedAlertSeverity, config)
}
