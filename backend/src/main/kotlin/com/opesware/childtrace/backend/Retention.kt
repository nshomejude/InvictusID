package com.opesware.childtrace.backend

import com.opesware.childtrace.backend.repo.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Bounded retention (invariant 3). Alerts and activity auto-expire after
 * [retentionDays]; consent records are retained as the accountability anchor
 * and are not pruned here. Runs a prune on start and then daily.
 *
 * The retention window and the ConsentRecord retention period are open items
 * for Cameroon counsel — [retentionDays] is a conservative default, not a
 * settled legal figure.
 */
class RetentionScheduler(
    private val store: Store,
    private val retentionDays: Long = 90,
    private val intervalMillis: Long = 24 * 60 * 60 * 1000,
    private val now: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val log = LoggerFactory.getLogger(RetentionScheduler::class.java)

    fun start() {
        scope.launch {
            while (true) {
                runCatching { pruneNow() }
                    .onFailure { log.warn("Retention prune failed", it) }
                delay(intervalMillis)
            }
        }
    }

    /** Runs one prune pass; returns rows removed. Exposed for tests. */
    fun pruneNow(): Int {
        val cutoff = now() - retentionDays * 24 * 60 * 60 * 1000
        val removed = store.pruneOlderThan(cutoff)
        if (removed > 0) log.info("Retention: pruned {} expired records older than {} days", removed, retentionDays)
        return removed
    }
}
