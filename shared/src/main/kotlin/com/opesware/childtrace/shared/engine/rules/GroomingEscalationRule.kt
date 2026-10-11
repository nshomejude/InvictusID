package com.opesware.childtrace.shared.engine.rules

import com.opesware.childtrace.shared.engine.RiskRule
import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Flag
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

/**
 * Grooming-pattern heuristic. Grooming rarely announces itself in a single
 * word; it shows up as an escalation pattern — an unknown contact pushing to
 * move platforms, asking for photos, demanding secrecy, or arranging to meet.
 *
 * This rule scores the message against several intent signals and escalates
 * severity when the sender is an unknown/first-contact party (the spec's
 * "new number → rapid intimacy"). It errs toward flagging because a missed
 * grooming signal is the most costly false negative in the product.
 */
class GroomingEscalationRule : RiskRule {

    private data class Signal(val weight: Int, val patterns: List<Regex>)

    private fun rx(vararg literals: String): List<Regex> =
        literals.map { Regex(it, RegexOption.IGNORE_CASE) }

    private val signals = listOf(
        // Push to move to another platform.
        Signal(2, rx(
            "\\b(let's|lets|can we|wanna|want to)\\s+(talk|chat|move|continue)\\s+on\\b",
            "\\b(add|message|text|dm)\\s+me\\s+on\\b",
            "\\bget\\s+(snap|snapchat|whatsapp|telegram|insta|instagram|kik)\\b",
        )),
        // Requests for images.
        Signal(3, rx(
            "\\bsend\\s+(me\\s+)?(a\\s+)?(pic|pics|picture|photo|photos|selfie|selfies)\\b",
            "\\bwhat\\s+(do\\s+)?you\\s+look\\s+like\\b",
            "\\bshow\\s+me\\b",
        )),
        // Secrecy / isolation.
        Signal(3, rx(
            "\\b(don'?t|do not)\\s+tell\\s+(your\\s+)?(mom|dad|parents|anyone)\\b",
            "\\b(our|this\\s+is\\s+our)\\s+secret\\b",
            "\\bdelete\\s+(this|these|the\\s+messages?)\\b",
        )),
        // Arranging to meet.
        Signal(3, rx(
            "\\b(meet|see)\\s+(up|me)\\b",
            "\\bcome\\s+(over|to\\s+my)\\b",
            "\\bi'?ll\\s+pick\\s+you\\s+up\\b",
            "\\bdon'?t\\s+(bring|tell)\\b.*\\b(anyone|parents)\\b",
        )),
        // Flattery + age probing, common opening moves.
        Signal(1, rx(
            "\\byou'?re\\s+(so\\s+)?(mature|grown\\s+up|special|different)\\b",
            "\\bhow\\s+old\\s+are\\s+you\\b",
            "\\bare\\s+you\\s+home\\s+alone\\b",
        )),
    )

    override fun evaluate(message: CapturedMessage): Flag? {
        val text = message.text
        var score = 0
        var hits = 0
        for (signal in signals) {
            if (signal.patterns.any { it.containsMatchIn(text) }) {
                score += signal.weight
                hits++
            }
        }
        if (score == 0) return null

        // An unknown contact exhibiting any of these raises the stakes sharply.
        if (message.senderIsUnknownContact) score += 2

        val severity = when {
            score >= 5 -> Severity.CRITICAL
            score >= 3 -> Severity.HIGH
            else -> Severity.MEDIUM
        }
        // Confidence grows with the number of independent signals, capped.
        val confidence = (0.35 + 0.18 * hits).coerceAtMost(0.95)

        val reason = buildString {
            append("Grooming-pattern signals detected")
            if (message.senderIsUnknownContact) append(" from an unknown contact")
            append(" (score $score, $hits signal${if (hits == 1) "" else "s"}).")
        }
        return Flag(RiskCategory.GROOMING, severity, confidence, reason)
    }
}
