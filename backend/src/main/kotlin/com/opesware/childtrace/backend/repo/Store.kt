package com.opesware.childtrace.backend.repo

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.Child
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import com.opesware.childtrace.shared.model.Geofence
import com.opesware.childtrace.shared.model.LocationSample
import com.opesware.childtrace.shared.model.TamperEvent

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

    // --- Phase 2 ---

    /** Register a child profile under a parent (multi-child). */
    fun saveChild(child: Child)
    /** All child profiles for a parent, for the multi-child switcher. */
    fun childrenOf(parentId: String): List<Child>
    /** One child profile by id (to resolve its parent account). */
    fun child(childId: String): Child?

    /** Store/replace the parent's FCM push token for a child; tokens are per child. */
    fun savePushToken(parentId: String, childId: String, token: String)
    /** All push tokens that should be notified for a child's critical alerts. */
    fun pushTokensFor(childId: String): List<String>

    fun saveGeofence(fence: Geofence)
    fun geofences(childId: String): List<Geofence>

    /** Append a location sample; also returns the previous sample for transition eval. */
    fun addLocation(sample: LocationSample)
    fun lastLocation(childId: String): LocationSample?
    fun locations(childId: String): List<LocationSample>

    fun addTamper(event: TamperEvent)
    fun tamperEvents(childId: String): List<TamperEvent>
}
