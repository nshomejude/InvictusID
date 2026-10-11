package com.opesware.childtrace.child.sync

import com.opesware.childtrace.child.data.ActivityStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Drains the local encrypted store to the backend on an interval: pull a batch
 * of pending records, upload them, mark them synced. Kept deliberately simple;
 * production would schedule this with WorkManager and back off on failure.
 */
class SyncManager(
    private val store: ActivityStore,
    private val client: BackendClient,
    private val intervalMillis: Long = 60_000,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    fun start() {
        scope.launch {
            while (true) {
                runCatching { drainOnce() }
                delay(intervalMillis)
            }
        }
    }

    suspend fun drainOnce() {
        val pending = store.pendingSync(limit = 100)
        if (pending.isEmpty()) return
        client.syncActivity(pending)
        store.markSynced(pending.map { it.id })
    }
}
