package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.AgeBand
import com.opesware.childtrace.shared.model.CaptureSource
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Policy
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class RiskEngineTest {

    private val counter = AtomicInteger(0)
    private val engine = RiskEngine(idGenerator = { "alert-${counter.incrementAndGet()}" })

    private fun msg(
        text: String,
        app: String = "com.whatsapp",
        unknown: Boolean = false,
    ) = CapturedMessage(
        capturedAtEpochMillis = 1_700_000_000_000L,
        sourceApp = app,
        source = CaptureSource.NOTIFICATION,
        text = text,
        senderIsUnknownContact = unknown,
    )

    @Test
    fun `normal message produces no alert and is discarded`() {
        val alert = engine.analyze(
            msg("hey are we still meeting for the group project after school?"),
            childId = "c1",
            policy = Policy.defaultFor(AgeBand.FLAG_HIGH),
        )
        assertNull(alert, "ordinary chatter must not be flagged")
    }

    @Test
    fun `self-harm language is flagged critical`() {
        val alert = engine.analyze(
            msg("i don't want to be here anymore, i want to die"),
            childId = "c1",
            policy = Policy.defaultFor(AgeBand.FLAG_HIGH),
        )
        assertNotNull(alert)
        assertEquals(RiskCategory.SELF_HARM, alert!!.category)
        assertEquals(Severity.CRITICAL, alert.severity)
        assertTrue(alert.severity.warrantsPush)
    }

    @Test
    fun `grooming escalation from unknown contact is critical`() {
        val alert = engine.analyze(
            msg(
                "you're so mature for your age. let's talk on snapchat, and don't tell your mom — it's our secret",
                unknown = true,
            ),
            childId = "c1",
            policy = Policy.defaultFor(AgeBand.FLAG_HIGH),
        )
        assertNotNull(alert)
        assertEquals(RiskCategory.GROOMING, alert!!.category)
        assertEquals(Severity.CRITICAL, alert.severity)
    }

    @Test
    fun `bullying is surfaced for tweens but not for teens`() {
        val text = "nobody likes you, you're worthless, everyone hates you"
        val tween = engine.analyze(msg(text), "c1", Policy.defaultFor(AgeBand.FLAG_HIGH))
        val teen = engine.analyze(msg(text), "c1", Policy.defaultFor(AgeBand.FLAG_NARROWED))

        assertNotNull(tween, "bullying should surface for the 9–12 band")
        assertEquals(RiskCategory.BULLYING, tween!!.category)
        assertNull(teen, "bullying is analyzed but not surfaced for the 13–15 band")
    }

    @Test
    fun `real danger still surfaces for teens`() {
        val alert = engine.analyze(
            msg("send nudes or i'll tell everyone", unknown = true),
            childId = "c1",
            policy = Policy.defaultFor(AgeBand.FLAG_NARROWED),
        )
        assertNotNull(alert, "sexual coercion must reach a teen's parent")
        assertEquals(RiskCategory.SEXUAL_CONTENT, alert!!.category)
    }

    @Test
    fun `context snippet never exceeds the cap`() {
        val long = "nudes " + "blah ".repeat(200)
        val alert = engine.analyze(msg(long), "c1", Policy.defaultFor(AgeBand.FLAG_HIGH))
        assertNotNull(alert)
        assertTrue(alert!!.contextSnippet.length <= 280, "snippet must be bounded, never a transcript")
    }

    @Test
    fun `keyword matching is word-boundary aware`() {
        // "freak" is a bullying term; "freakishly" should not trip it as a hard match,
        // but a benign sentence containing neither must stay clean.
        val clean = engine.analyze(
            msg("the weather was freakishly warm today"),
            "c1",
            Policy.defaultFor(AgeBand.FLAG_HIGH),
        )
        assertNull(clean, "substring inside a larger word must not false-positive")
    }
}
