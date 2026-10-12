package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Flag
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

/**
 * An on-device text classifier. The launch engine is rules-based; this is the
 * seam where a TensorFlow Lite model plugs in without touching the rest of the
 * engine. The model runs entirely on the child's phone — only its flagged
 * output ever leaves the device.
 */
fun interface Classifier {
    /** Per-category probabilities in [0,1] for the message text. */
    fun classify(text: String): Map<RiskCategory, Double>
}

/**
 * Wraps a [Classifier] as a [RiskRule], firing when a category's probability
 * crosses [threshold]. This lets the model and the rules coexist in one engine:
 * the rules give explainable coverage today, the model adds recall as it matures.
 */
class ClassifierRule(
    private val classifier: Classifier,
    private val threshold: Double = 0.8,
    private val severityFor: (RiskCategory, Double) -> Severity = { _, p ->
        when {
            p >= 0.95 -> Severity.CRITICAL
            p >= 0.88 -> Severity.HIGH
            else -> Severity.MEDIUM
        }
    },
) : RiskRule {
    override fun evaluate(message: CapturedMessage): Flag? {
        val scores = classifier.classify(message.text)
        val top = scores.maxByOrNull { it.value } ?: return null
        if (top.value < threshold) return null
        return Flag(
            category = top.key,
            severity = severityFor(top.key, top.value),
            confidence = top.value,
            reason = "On-device model flagged ${top.key.label.lowercase()} " +
                "(p=${"%.2f".format(top.value)}).",
        )
    }
}
