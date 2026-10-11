package com.opesware.childtrace.child.data

import com.opesware.childtrace.shared.model.ActivityRecord

/**
 * Local, encrypted store of captured activity awaiting sync. Backed by Room on
 * an Android Keystore-protected database. Records are held only until synced
 * and then pruned per the retention policy; the context of a flagged item is
 * treated as the most sensitive field.
 */
interface ActivityStore {
    suspend fun record(activity: ActivityRecord)
    suspend fun pendingSync(limit: Int = 100): List<ActivityRecord>
    suspend fun markSynced(ids: List<String>)
}
