package com.opesware.childtrace.backend

import com.opesware.childtrace.backend.repo.AuditAction
import com.opesware.childtrace.backend.repo.AuditEntry
import com.opesware.childtrace.backend.repo.AuditResource
import com.opesware.childtrace.backend.repo.InMemoryStore
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RetentionAuditTest {

    private val dayMs = 24L * 60 * 60 * 1000

    private fun record(id: String, ageDays: Long, now: Long) = ActivityRecord(
        id = id, childId = "c1", type = ActivityType.MESSAGE,
        createdAtEpochMillis = now - ageDays * dayMs,
        sourceApp = "com.whatsapp", content = "x",
    )

    @Test
    fun `prune removes records older than the window and keeps recent ones`() {
        val now = 10_000L * dayMs
        val store = InMemoryStore()
        store.addActivity("c1", listOf(record("old", 120, now), record("fresh", 10, now)))

        val removed = RetentionScheduler(store, retentionDays = 90, now = { now }).pruneNow()

        assertEquals(1, removed)
        val remaining = store.activity("c1").map { it.id }
        assertEquals(listOf("fresh"), remaining)
    }

    @Test
    fun `audit entries are recorded and returned most-recent-first`() {
        val store = InMemoryStore()
        store.recordAccess(AuditEntry(1000, "agent-a", AuditAction.WRITE, AuditResource.CONSENT, "c1"))
        store.recordAccess(AuditEntry(2000, "parent-1", AuditAction.READ, AuditResource.ACTIVITY, "c1"))

        val trail = store.auditTrail("c1")
        assertEquals(2, trail.size)
        assertEquals(2000, trail.first().atEpochMillis)
        assertEquals(AuditResource.ACTIVITY, trail.first().resource)
    }
}
