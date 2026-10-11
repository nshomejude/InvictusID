package com.opesware.childtrace.backend.repo

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe in-memory stand-in for the PostgreSQL store, so the API runs
 * and is testable before the database is wired. Replace with an Exposed-backed
 * repository implementing the same surface.
 *
 * Alerts and activity are scoped per child; nothing is shared across children,
 * and nothing is ever exported for resale.
 */
class InMemoryStore : Store {
    private val devices = ConcurrentHashMap<String, Device>()
    private val consents = ConcurrentHashMap<String, ConsentRecord>()
    private val alerts = ConcurrentHashMap<String, MutableList<Alert>>()
    private val activity = ConcurrentHashMap<String, MutableList<ActivityRecord>>()

    override fun saveDevice(device: Device) { devices[device.id] = device }
    override fun device(id: String): Device? = devices[id]

    override fun saveConsent(record: ConsentRecord) { consents[record.childId] = record }
    override fun consent(childId: String): ConsentRecord? = consents[childId]

    override fun addAlerts(childId: String, items: List<Alert>) {
        alerts.getOrPut(childId) { mutableListOf() }.addAll(items)
    }
    override fun alerts(childId: String): List<Alert> =
        alerts[childId].orEmpty().sortedByDescending { it.severity.rank }

    override fun addActivity(childId: String, items: List<ActivityRecord>) {
        activity.getOrPut(childId) { mutableListOf() }.addAll(items)
    }
    override fun activity(childId: String): List<ActivityRecord> =
        // Flagged items first (danger ranked up), then most recent.
        activity[childId].orEmpty().sortedWith(
            compareByDescending<ActivityRecord> { it.isFlagged }
                .thenByDescending { it.createdAtEpochMillis }
        )
}
