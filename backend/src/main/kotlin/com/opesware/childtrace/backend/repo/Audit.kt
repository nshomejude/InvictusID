package com.opesware.childtrace.backend.repo

/**
 * An audit entry recording access to a child's data. Invariant 3 requires that
 * every read and write of alerts, activity, and consent records is logged —
 * this is the accountability trail a regulator or court can inspect, and a
 * deterrent against misuse of a tool that holds a child's comprehensive data.
 */
data class AuditEntry(
    val atEpochMillis: Long,
    val actor: String,
    val action: AuditAction,
    val resource: AuditResource,
    val childId: String,
    val detail: String? = null,
)

enum class AuditAction { READ, WRITE }

enum class AuditResource { ALERT, ACTIVITY, CONSENT, DEVICE }
