package com.opesware.childtrace.child.capture

import com.opesware.childtrace.shared.model.CapturedMessage

/**
 * Where both capture feeds hand off their [CapturedMessage]s. The app binds a
 * single implementation (the on-device analyzer) so the services stay thin and
 * free of analysis or storage logic.
 */
fun interface CaptureSink {
    fun submit(message: CapturedMessage)
}
