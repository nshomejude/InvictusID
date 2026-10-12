package com.opesware.childtrace.shared

import com.opesware.childtrace.shared.engine.ClassifierRule
import com.opesware.childtrace.shared.engine.DigestBuilder
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.AlertStatus
import com.opesware.childtrace.shared.model.CaptureSource
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Geofence
import com.opesware.childtrace.shared.model.GeofenceEvent
import com.opesware.childtrace.shared.model.GeofenceTrigger
import com.opesware.childtrace.shared.model.Geofencing
import com.opesware.childtrace.shared.model.LocationSample
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.SafetyResources
import com.opesware.childtrace.shared.model.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase2Test {

    // --- Geofencing ---
    private val school = Geofence("g1", "c1", "School", 4.0611, 9.7679, 150.0, GeofenceTrigger.BOTH)
    private fun at(lat: Double, lon: Double, t: Long = 0) = LocationSample("c1", lat, lon, t)

    @Test
    fun `geofence detects exit transition`() {
        val inside = at(4.0611, 9.7679)
        val faraway = at(4.0700, 9.7800)
        assertEquals(GeofenceEvent.EXITED, Geofencing.evaluate(school, inside, faraway))
    }

    @Test
    fun `geofence detects enter transition`() {
        val outside = at(4.0700, 9.7800)
        val inside = at(4.0611, 9.7679)
        assertEquals(GeofenceEvent.ENTERED, Geofencing.evaluate(school, outside, inside))
    }

    @Test
    fun `no transition on first fix or when staying inside`() {
        assertNull(Geofencing.evaluate(school, null, at(4.0611, 9.7679)))
        assertNull(Geofencing.evaluate(school, at(4.0611, 9.7679), at(4.0612, 9.7680)))
    }

    @Test
    fun `enter-only fence ignores exits`() {
        val enterOnly = school.copy(trigger = GeofenceTrigger.ENTER)
        assertNull(Geofencing.evaluate(enterOnly, at(4.0611, 9.7679), at(4.0700, 9.7800)))
    }

    // --- Classifier rule ---
    @Test
    fun `classifier rule fires above threshold and is quiet below`() {
        val rule = ClassifierRule(
            classifier = { text ->
                if (text.contains("meet")) mapOf(RiskCategory.GROOMING to 0.97) else mapOf(RiskCategory.GROOMING to 0.1)
            },
            threshold = 0.8,
        )
        val msg = { t: String -> CapturedMessage(0, "com.x", CaptureSource.NOTIFICATION, t) }
        val hit = rule.evaluate(msg("let's meet after school"))
        assertNotNull(hit)
        assertEquals(Severity.CRITICAL, hit!!.severity)
        assertNull(rule.evaluate(msg("doing homework")))
    }

    // --- Digest ---
    @Test
    fun `digest aggregates a window and headlines critical`() {
        fun alert(sev: Severity, cat: RiskCategory, t: Long) =
            Alert("a$t", "c1", cat, sev, t, "com.whatsapp", "x", "y", AlertStatus.NEW)
        val alerts = listOf(
            alert(Severity.CRITICAL, RiskCategory.GROOMING, 10),
            alert(Severity.MEDIUM, RiskCategory.BULLYING, 20),
            alert(Severity.LOW, RiskCategory.BULLYING, 5000), // outside window
        )
        val d = DigestBuilder.build("c1", alerts, windowStart = 0, windowEnd = 1000)
        assertEquals(2, d.totalFlags)
        assertEquals("com.whatsapp", d.mostActiveApp)
        assertTrue(d.headline.contains("urgent"))
    }

    // --- Safety resources ---
    @Test
    fun `self-harm surfaces a child-facing resource`() {
        val res = SafetyResources.forCategory(RiskCategory.SELF_HARM, "CM")
        assertTrue(res.isNotEmpty())
        assertTrue(res.any { it.childFacing })
    }

    @Test
    fun `bullying has no crisis pathway`() {
        assertTrue(SafetyResources.forCategory(RiskCategory.BULLYING).isEmpty())
    }
}
