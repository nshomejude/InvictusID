package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/**
 * Raised when ChildTrace detects that its own capture was interfered with on
 * the child device — a monitored service disabled, permissions revoked, the app
 * force-stopped. The parent is told openly; this is tamper-*resistance*, never
 * concealment. The child can still see and uninstall the app (invariant 1).
 */
@Serializable
data class TamperEvent(
    val childId: String,
    val kind: TamperKind,
    val atEpochMillis: Long,
    val detail: String? = null,
)

@Serializable
enum class TamperKind {
    NOTIFICATION_ACCESS_REVOKED,
    ACCESSIBILITY_DISABLED,
    CAPTURE_SERVICE_STOPPED,
    LOCATION_PERMISSION_REVOKED,
    DEVICE_ADMIN_REMOVED,
}
