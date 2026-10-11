package com.opesware.childtrace.shared.engine.rules

import com.opesware.childtrace.shared.engine.RiskRule
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Flag
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

/**
 * A lexical rule that fires when any of its [phrases] appears in the message
 * text (case-insensitive, word-boundary aware). This is the deterministic
 * backbone of the launch engine — explainable, testable, and good enough to
 * beat a parent scrolling manually while the ML classifier matures.
 *
 * Phrase matching is intentionally conservative: it matches whole words/phrases
 * to avoid the classic "scunthorpe" substring false positives.
 */
class KeywordRule(
    private val category: RiskCategory,
    private val severity: Severity,
    private val confidence: Double,
    private val reason: String,
    phrases: Collection<String>,
) : RiskRule {

    private val patterns: List<Regex> = phrases.map { phrase ->
        // Escape the phrase, then require a non-word boundary on each side.
        Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(phrase) + "(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
    }

    override fun evaluate(message: CapturedMessage): Flag? {
        val text = message.text
        val matched = patterns.any { it.containsMatchIn(text) }
        return if (matched) Flag(category, severity, confidence, reason) else null
    }
}
