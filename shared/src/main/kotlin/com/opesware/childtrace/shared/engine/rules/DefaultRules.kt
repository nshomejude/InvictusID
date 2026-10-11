package com.opesware.childtrace.shared.engine.rules

import com.opesware.childtrace.shared.engine.RiskRule
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

/**
 * The launch rule set. These are deliberately small, explainable heuristics —
 * the "good rules layer plus a modest classifier already beats manual
 * scrolling" baseline from the spec. Phrase lists are starting points to be
 * tuned against pilot data and localized (the product ships EN + FR).
 *
 * NOTE: these lists contain clinical/safety terms by necessity. They are data,
 * not an endorsement, and exist solely to route a child-safety alert to a parent.
 */
object DefaultRules {

    fun all(): List<RiskRule> = buildList {
        add(GroomingEscalationRule())

        add(
            KeywordRule(
                category = RiskCategory.SELF_HARM,
                severity = Severity.CRITICAL,
                confidence = 0.8,
                reason = "Language associated with self-harm or suicidal ideation.",
                phrases = listOf(
                    "kill myself", "want to die", "end it all", "cut myself",
                    "better off dead", "no reason to live", "hurt myself",
                    "suicidal", "don't want to be here anymore",
                ),
            )
        )

        add(
            KeywordRule(
                category = RiskCategory.SEXUAL_CONTENT,
                severity = Severity.HIGH,
                confidence = 0.7,
                reason = "Sexual content directed at or from the child.",
                phrases = listOf(
                    "nudes", "send nudes", "sexting", "horny",
                    "take your clothes off", "naked pic",
                ),
            )
        )

        add(
            KeywordRule(
                category = RiskCategory.VIOLENCE_THREAT,
                severity = Severity.HIGH,
                confidence = 0.7,
                reason = "Threat of violence.",
                phrases = listOf(
                    "i'll kill you", "going to hurt you", "beat you up",
                    "i know where you live", "watch your back",
                ),
            )
        )

        add(
            KeywordRule(
                category = RiskCategory.BULLYING,
                severity = Severity.MEDIUM,
                confidence = 0.55,
                reason = "Language associated with bullying or harassment.",
                phrases = listOf(
                    "kill yourself", "kys", "nobody likes you", "you're worthless",
                    "everyone hates you", "loser", "freak",
                ),
            )
        )
    }
}
