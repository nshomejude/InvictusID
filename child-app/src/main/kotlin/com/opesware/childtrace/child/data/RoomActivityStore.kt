package com.opesware.childtrace.child.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity

@Entity(tableName = "activity")
data class ActivityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("child_id") val childId: String,
    val type: String,
    @ColumnInfo("created_at") val createdAt: Long,
    @ColumnInfo("source_app") val sourceApp: String?,
    val content: String,
    val counterparty: String?,
    @ColumnInfo("risk_category") val riskCategory: String?,
    val severity: String?,
    val synced: Boolean = false,
) {
    fun toModel() = ActivityRecord(
        id = id,
        childId = childId,
        type = ActivityType.valueOf(type),
        createdAtEpochMillis = createdAt,
        sourceApp = sourceApp,
        content = content,
        counterparty = counterparty,
        riskCategory = riskCategory?.let { RiskCategory.valueOf(it) },
        severity = severity?.let { Severity.valueOf(it) },
    )

    companion object {
        fun from(a: ActivityRecord) = ActivityEntity(
            id = a.id,
            childId = a.childId,
            type = a.type.name,
            createdAt = a.createdAtEpochMillis,
            sourceApp = a.sourceApp,
            content = a.content,
            counterparty = a.counterparty,
            riskCategory = a.riskCategory?.name,
            severity = a.severity?.name,
        )
    }
}

@Dao
interface ActivityDao {
    @Insert
    suspend fun insert(entity: ActivityEntity)

    @Query("SELECT * FROM activity WHERE synced = 0 ORDER BY created_at ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<ActivityEntity>

    @Query("UPDATE activity SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)
}

@Database(entities = [ActivityEntity::class], version = 1, exportSchema = false)
abstract class ChildDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
}

/**
 * Room-backed [ActivityStore]. The database file is opened on an Android
 * Keystore-derived key (SQLCipher / encrypted passphrase); that wiring lives in
 * [com.opesware.childtrace.child.ChildApplication] so this class stays a thin
 * adapter over the DAO.
 */
class RoomActivityStore(private val dao: ActivityDao) : ActivityStore {
    override suspend fun record(activity: ActivityRecord) = dao.insert(ActivityEntity.from(activity))
    override suspend fun pendingSync(limit: Int): List<ActivityRecord> = dao.pending(limit).map { it.toModel() }
    override suspend fun markSynced(ids: List<String>) = dao.markSynced(ids)
}
