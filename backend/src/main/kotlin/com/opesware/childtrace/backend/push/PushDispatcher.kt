package com.opesware.childtrace.backend.push

import com.opesware.childtrace.shared.model.Alert
import org.slf4j.LoggerFactory

/**
 * Sends a real-time push to the parent when a critical danger flag arrives, so
 * it reaches them in seconds rather than at the next poll. The payload carries
 * no content — only "a critical alert arrived for child X" — because the server
 * holds ciphertext; the parent app fetches and decrypts the detail.
 */
fun interface PushDispatcher {
    fun notifyCritical(tokens: List<String>, alert: Alert)
}

/**
 * Default dispatcher: logs the intent. Swap for an FCM-backed implementation
 * (FirebaseMessaging) in production via the FCM_SERVER_KEY env var. Kept behind
 * the interface so the server runs and tests without Firebase credentials.
 */
class LoggingPushDispatcher : PushDispatcher {
    private val log = LoggerFactory.getLogger(LoggingPushDispatcher::class.java)
    override fun notifyCritical(tokens: List<String>, alert: Alert) {
        if (tokens.isEmpty()) return
        log.info(
            "PUSH critical alert {} for child {} to {} device(s) (no content in payload)",
            alert.category.name, alert.childId, tokens.size,
        )
    }
}
