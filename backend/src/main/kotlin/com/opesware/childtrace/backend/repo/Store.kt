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

    fun addAlerts(childId: String, items: List<Alert>)
    /** Alerts for a child, severity-ranked (critical first). */
    fun alerts(childId: String): List<Alert>

    fun addActivity(childId: String, items: List<ActivityRecord>)
    /** Activity for a child, flagged items first then most recent. */
    fun activity(childId: String): List<ActivityRecord>
}
