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
    private val audit = ConcurrentHashMap<String, MutableList<AuditEntry>>()
    // childId -> (keyId -> spki), plus the current keyId per child.
    private val parentKeys = ConcurrentHashMap<String, MutableMap<String, String>>()
    private val currentKeyId = ConcurrentHashMap<String, String>()

    override fun saveDevice(device: Device) { devices[device.id] = device }
    override fun device(id: String): Device? = devices[id]

    override fun saveConsent(record: ConsentRecord) { consents[record.childId] = record }
    override fun consent(childId: String): ConsentRecord? = consents[childId]

    override fun saveParentKey(childId: String, keyId: String, publicKeySpki: String) {
        parentKeys.getOrPut(childId) { mutableMapOf() }[keyId] = publicKeySpki
        currentKeyId[childId] = keyId
    }
    override fun currentParentKey(childId: String): Pair<String, String>? {
        val keyId = currentKeyId[childId] ?: return null
        val spki = parentKeys[childId]?.get(keyId) ?: return null
        return keyId to spki
    }

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

    override fun recordAccess(entry: AuditEntry) {
        audit.getOrPut(entry.childId) { mutableListOf() }.add(entry)
    }

    override fun auditTrail(childId: String): List<AuditEntry> =
        audit[childId].orEmpty().sortedByDescending { it.atEpochMillis }

    override fun pruneOlderThan(cutoffEpochMillis: Long): Int {
        var removed = 0
        alerts.values.forEach { list ->
            val before = list.size
            list.removeAll { it.createdAtEpochMillis < cutoffEpochMillis }
            removed += before - list.size
        }
        activity.values.forEach { list ->
            val before = list.size
            list.removeAll { it.createdAtEpochMillis < cutoffEpochMillis }
            removed += before - list.size
        }
        return removed
    }
}
