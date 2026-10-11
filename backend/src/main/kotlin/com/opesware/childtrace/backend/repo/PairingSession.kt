package com.opesware.childtrace.backend.repo

/**
 * A short-lived, one-time pairing session that sequences onboarding:
 *
 *   PENDING         → agent created it for a provisioned child
 *   PARENT_CLAIMED  → parent entered the code and registered its key
 *   COMPLETED       → child device paired; capture may begin
 *
 * The order is enforced: the child can only pair once the parent has claimed,
 * so there is always a parent key to seal content to. Sessions expire, so a
 * leaked code is useless after the window.
 */
data class PairingSession(
    val code: String,
    val childId: String,
    val status: PairingStatus,
    val createdAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
) {
    fun isExpired(now: Long): Boolean = now >= expiresAtEpochMillis
}

enum class PairingStatus { PENDING, PARENT_CLAIMED, COMPLETED }
