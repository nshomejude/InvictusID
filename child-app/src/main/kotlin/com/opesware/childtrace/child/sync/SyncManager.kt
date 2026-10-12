package com.opesware.childtrace.child.sync

import com.opesware.childtrace.child.data.ActivityStore
import com.opesware.childtrace.child.tamper.TamperMonitor

/**
 * Drains the local encrypted store to the backend and reports any visible
 * tamper (disabled capture, revoked permissions) so the parent is told openly.
 * Cadence, retry, and backoff are handled by [SyncWorker] via WorkManager; this
 * holds the one-pass logic so it stays easy to test and reuse.
 */
class SyncManager(
    private val store: ActivityStore,
    private val client: BackendClient,
    private val tamperMonitor: TamperMonitor? = null,
) {
    suspend fun drainOnce() {
        tamperMonitor?.scan()?.takeIf { it.isNotEmpty() }?.let { client.reportTamper(it) }
        val pending = store.pendingSync(limit = 100)
        if (pending.isEmpty()) return
        client.syncActivity(pending)
        store.markSynced(pending.map { it.id })
    }
}
