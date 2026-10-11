package com.opesware.childtrace.shared.model

/**
 * Severity of a flagged item, used to rank the parent's alert feed (critical at
 * the top) and to decide whether a push notification fires.
 */
enum class Severity(val rank: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    /** Critical flags warrant an immediate push to the parent. */
    val warrantsPush: Boolean get() = this == CRITICAL
}
