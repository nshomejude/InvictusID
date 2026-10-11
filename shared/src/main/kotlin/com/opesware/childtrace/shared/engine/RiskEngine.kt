package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.engine.rules.DefaultRules
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.AlertStatus
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Flag
import com.opesware.childtrace.shared.model.Policy
import com.opesware.childtrace.shared.model.Severity

/**
 * The on-device risk engine — the actual product.
 *
 * Every captured message is scored here *on the child's phone*. If nothing
 * fires, or what fires does not clear the age band's sensitivity threshold, the
 * message is discarded and nothing leaves the device. Only when an [Alert] is
 * produced does anything sync to the backend.
 *
 * This class is pure and deterministic: no I/O, no Android dependency. That is
 * what lets it be unit-tested exhaustively on any JVM and reused unchanged by
 * the child-app's Android analyzer.
 */
class RiskEngine(
    private val rules: List<RiskRule> = DefaultRules.all(),
    private val idGenerator: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    /**
     * Analyze one captured message under a child's [policy].
     *
     * @return an [Alert] to surface to the parent, or null if the message is
     *   normal (or below the band's surfacing threshold) and should be discarded.
     */
    fun analyze(message: CapturedMessage, childId: String, policy: Policy): Alert? {
        val flags = rules.mapNotNull { it.evaluate(message) }
        if (flags.isEmpty()) return null

        // The dominant flag drives the alert: highest severity, then highest
        // confidence. All flags inform the reason.
        val dominant = flags.maxWith(
            compareBy<Flag> { it.severity.rank }.thenBy { it.confidence }
        )

        // Age-band gate 1: for the narrowed (13–15) band, categories that are
        // everyday-chatter for a teen (bullying, generic stranger contact) are
        // analyzed but never surfaced. Real danger still gets through.
        if (policy.band == com.opesware.childtrace.shared.model.AgeBand.FLAG_NARROWED &&
            !dominant.category.surfacesInNarrowedBand
        ) {
            return null
        }

        // Age-band gate 2: severity must clear the band's minimum surfaced level.
        if (dominant.severity.rank < policy.minSurfacedSeverity.rank) return null

        return Alert(
            id = idGenerator(),
            childId = childId,
            category = dominant.category,
            severity = dominant.severity,
            createdAtEpochMillis = message.capturedAtEpochMillis,
            sourceApp = message.sourceApp,
            contextSnippet = snippet(message.text),
            reason = flags.joinToString(" ") { it.reason },
            status = AlertStatus.NEW,
        )
    }

    /**
     * Minimal context, never a transcript. We keep a bounded window around the
     * flagged text so the parent can understand it, and cap the length so a long
     * message can't smuggle a whole conversation into the alert.
     */
    private fun snippet(text: String, maxLen: Int = 280): String {
        val collapsed = text.replace(Regex("\\s+"), " ").trim()
        return if (collapsed.length <= maxLen) collapsed else collapsed.take(maxLen - 1) + "…"
    }

    companion object {
        /** Convenience: does this severity warrant an immediate parent push? */
        fun warrantsPush(severity: Severity): Boolean = severity.warrantsPush
    }
}
