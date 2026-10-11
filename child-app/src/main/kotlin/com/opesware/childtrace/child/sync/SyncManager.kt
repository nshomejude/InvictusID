package com.opesware.childtrace.child.sync

import com.opesware.childtrace.child.data.ActivityStore

/**
 * Drains the local encrypted store to the backend: pull a batch of pending
 * records, upload them (sealed to the parent's key by [BackendClient]), mark
 * them synced. Cadence, retry, and backoff are handled by [SyncWorker] via
 * WorkManager; this class holds just the one-pass logic so it stays easy to
 * test and reuse.
 */
class SyncManager(
    private val store: ActivityStore,
    private val client: BackendClient,
) {
    suspend fun drainOnce() {
        val pending = store.pendingSync(limit = 100)
        if (pending.isEmpty()) return
        client.syncActivity(pending)
        store.markSynced(pending.map { it.id })
    }
}
