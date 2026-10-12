package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.MediaKind
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.Severity
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class MediaPolicyTest {

    // --- Flagged media: allowed only as the context of a high-enough alert ---

    @ParameterizedTest
    @EnumSource(names = ["HIGH", "CRITICAL"])
    fun `flagged media is allowed for alerts at or above the floor`(sev: Severity) {
        val d = MediaPolicy.evaluate(MediaKind.IMAGE, MediaTrigger.FLAGGED_ALERT, sev)
        assertInstanceOf(MediaPolicy.Decision.Allowed::class.java, d)
    }

    @ParameterizedTest
    @EnumSource(names = ["LOW", "MEDIUM"])
    fun `flagged media is refused for alerts below the floor`(sev: Severity) {
        val d = MediaPolicy.evaluate(MediaKind.IMAGE, MediaTrigger.FLAGGED_ALERT, sev)
        assertInstanceOf(MediaPolicy.Decision.Refused::class.java, d)
    }

    @Test
    fun `flagged media with no linked alert is refused — this is the no-bulk guard`() {
        val d = MediaPolicy.evaluate(MediaKind.IMAGE, MediaTrigger.FLAGGED_ALERT, linkedAlertSeverity = null)
        assertInstanceOf(MediaPolicy.Decision.Refused::class.java, d)
    }

    @Test
    fun `a lowered floor lets medium-alert context through`() {
        val cfg = MediaPolicy.Config(severityFloor = Severity.MEDIUM)
        val d = MediaPolicy.evaluate(MediaKind.IMAGE, MediaTrigger.FLAGGED_ALERT, Severity.MEDIUM, cfg)
        assertInstanceOf(MediaPolicy.Decision.Allowed::class.java, d)
    }

    // --- Explicit screen capture: counsel-gated, screenshot only ---

    @Test
    fun `screen capture is refused by default`() {
        val d = MediaPolicy.evaluate(MediaKind.SCREENSHOT, MediaTrigger.PARENT_REQUESTED_SCREEN, null)
        assertInstanceOf(MediaPolicy.Decision.Refused::class.java, d)
    }

    @Test
    fun `screen capture is allowed only when the gate is on`() {
        val cfg = MediaPolicy.Config(allowScreenCapture = true)
        val d = MediaPolicy.evaluate(MediaKind.SCREENSHOT, MediaTrigger.PARENT_REQUESTED_SCREEN, null, cfg)
        assertInstanceOf(MediaPolicy.Decision.Allowed::class.java, d)
    }

    @ParameterizedTest
    @EnumSource(names = ["IMAGE", "VIDEO"])
    fun `parent-requested screen trigger only ever yields a screenshot`(kind: MediaKind) {
        val cfg = MediaPolicy.Config(allowScreenCapture = true)
        val d = MediaPolicy.evaluate(kind, MediaTrigger.PARENT_REQUESTED_SCREEN, null, cfg)
        assertInstanceOf(MediaPolicy.Decision.Refused::class.java, d)
    }
}
