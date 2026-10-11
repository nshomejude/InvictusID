package com.opesware.childtrace.backend.repo

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device

/**
 * The backend's persistence surface. Routes depend on this, not on a concrete
 * store, so the in-memory stand-in and the PostgreSQL/Exposed implementation
 * are interchangeable.
 *
 * Every read is scoped to a single child; nothing crosses children and nothing
 * is exported for resale.
 */
interface Store {
    fun saveDevice(device: Device)
    fun device(id: String): Device?

    fun saveConsent(record: ConsentRecord)
    fun consent(childId: String): ConsentRecord?

    fun savePairingSession(session: PairingSession)
    fun pairingSession(code: String): PairingSession?

    /**
     * Register a parent public key for a child. Registering rotates: the new
     * key becomes current and any prior key for the child is marked not-current
     * (its id is retained for history).
     */
    fun saveParentKey(childId: String, keyId: String, publicKeySpki: String)
    /** The child's *current* parent key as (keyId, spki), or null if none registered. */
    fun currentParentKey(childId: String): Pair<String, String>?

    fun addAlerts(childId: String, items: List<Alert>)
    /** Alerts for a child, severity-ranked (critical first). */
    fun alerts(childId: String): List<Alert>

    fun addActivity(childId: String, items: List<ActivityRecord>)
    /** Activity for a child, flagged items first then most recent. */
    fun activity(childId: String): List<ActivityRecord>

    /** Append an access-audit entry (invariant 3). */
    fun recordAccess(entry: AuditEntry)
    /** Audit trail for a child, most recent first. */
    fun auditTrail(childId: String): List<AuditEntry>

    /**
     * Delete alerts and activity older than [cutoffEpochMillis]. Consent records
     * are retained (the accountability anchor) and not pruned here. Returns the
     * number of rows removed.
     */
    fun pruneOlderThan(cutoffEpochMillis: Long): Int
}
