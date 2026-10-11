package com.opesware.childtrace.shared.model

/**
 * The output of a single [com.opesware.childtrace.shared.engine.RiskRule] when it
 * fires on a captured message. A rule contributes a category, a severity, a
 * confidence in [0,1], and a short human-readable reason.
 *
 * Flags are intermediate: the engine combines all flags for a message into at
 * most one [Alert] (or discards the message if nothing fired).
 */
data class Flag(
    val category: RiskCategory,
    val severity: Severity,
    val confidence: Double,
    val reason: String,
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be in [0,1], was $confidence" }
    }
}
