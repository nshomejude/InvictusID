package com.opesware.childtrace.shared.engine

import com.opesware.childtrace.shared.model.CapturedMessage
import com.opesware.childtrace.shared.model.Flag

/**
 * A single risk heuristic. The launch engine is a *rules + lightweight
 * classifier hybrid* (per the spec's "do not block the first build on a perfect
 * model"); each rule is a small, independently testable unit that inspects a
 * captured message and optionally returns a [Flag].
 *
 * Rules run entirely on-device and must be cheap and side-effect-free. A future
 * TensorFlow Lite classifier plugs in as just another [RiskRule].
 */
fun interface RiskRule {
    /** Returns a flag if this rule fires on [message], or null otherwise. */
    fun evaluate(message: CapturedMessage): Flag?
}
