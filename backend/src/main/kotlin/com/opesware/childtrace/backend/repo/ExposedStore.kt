package com.opesware.childtrace.backend.repo

import com.opesware.childtrace.shared.api.MediaRef
import com.opesware.childtrace.shared.api.SealedMediaResponse
import com.opesware.childtrace.shared.crypto.SealedEnvelope
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.AlertStatus
import com.opesware.childtrace.shared.model.CapturedMedia
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import com.opesware.childtrace.shared.model.MediaKind
import com.opesware.childtrace.shared.model.MediaTrigger
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * PostgreSQL-backed [Store] using Exposed over a Hikari pool. Severity/category
 * ranking is done in the application after load, keeping the SQL simple and the
 * ordering identical to the in-memory store.
 */
class ExposedStore(jdbcUrl: String, user: String, password: String) : Store {

    private val db: Database

    init {
        val cfg = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 8
        }
        db = Database.connect(HikariDataSource(cfg))
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(Devices, Consents, Alerts, Activities, AuditLog, ParentKeys, PairingSessions, MediaItems)
        }
    }

    override fun saveDevice(device: Device) = transaction(db) {
        Devices.deleteWhere { Devices.id eq device.id }
        Devices.insert {
            it[id] = device.id
            it[childId] = device.childId
            it[pairingToken] = device.pairingToken
            it[appVersion] = device.appVersion
            it[captureActive] = device.captureActive
        }
        Unit
    }

    override fun device(id: String): Device? = transaction(db) {
        Devices.selectAll().where { Devices.id eq id }.singleOrNull()?.let {
            Device(it[Devices.id], it[Devices.childId], it[Devices.pairingToken], it[Devices.appVersion], it[Devices.captureActive])
        }
    }

    override fun saveConsent(record: ConsentRecord) = transaction(db) {
        Consents.deleteWhere { Consents.childId eq record.childId }
        Consents.insert {
            it[id] = record.id
            it[childId] = record.childId
            it[buyerName] = record.buyerName
            it[buyerAge] = record.buyerAge
            it[guardianshipProofRef] = record.guardianshipProofRef
            it[signedCertificateRef] = record.signedCertificateRef
            it[provisionedAt] = record.provisionedAtEpochMillis
            it[childAgeAtProvisioning] = record.childAgeAtProvisioning
        }
        Unit
    }

    override fun consent(childId: String): ConsentRecord? = transaction(db) {
        Consents.selectAll().where { Consents.childId eq childId }.singleOrNull()?.let {
            ConsentRecord(
                id = it[Consents.id],
                childId = it[Consents.childId],
                buyerName = it[Consents.buyerName],
                buyerAge = it[Consents.buyerAge],
                guardianshipProofRef = it[Consents.guardianshipProofRef],
                signedCertificateRef = it[Consents.signedCertificateRef],
                provisionedAtEpochMillis = it[Consents.provisionedAt],
                childAgeAtProvisioning = it[Consents.childAgeAtProvisioning],
            )
        }
    }

    override fun savePairingSession(session: PairingSession) = transaction(db) {
        PairingSessions.deleteWhere { PairingSessions.code eq session.code }
        PairingSessions.insert {
            it[code] = session.code
            it[childId] = session.childId
            it[status] = session.status.name
            it[createdAt] = session.createdAtEpochMillis
            it[expiresAt] = session.expiresAtEpochMillis
        }
        Unit
    }

    override fun pairingSession(code: String): PairingSession? = transaction(db) {
        PairingSessions.selectAll().where { PairingSessions.code eq code }.singleOrNull()?.let {
            PairingSession(
                code = it[PairingSessions.code],
                childId = it[PairingSessions.childId],
                status = PairingStatus.valueOf(it[PairingSessions.status]),
                createdAtEpochMillis = it[PairingSessions.createdAt],
                expiresAtEpochMillis = it[PairingSessions.expiresAt],
            )
        }
    }

    override fun saveParentKey(childId: String, keyId: String, publicKeySpki: String) = transaction(db) {
        // Demote any existing current key, then upsert this one as current.
        ParentKeys.update({ ParentKeys.childId eq childId }) { it[isCurrent] = false }
        ParentKeys.deleteWhere { (ParentKeys.childId eq childId) and (ParentKeys.keyId eq keyId) }
        ParentKeys.insert {
            it[ParentKeys.childId] = childId
            it[ParentKeys.keyId] = keyId
            it[ParentKeys.publicKeySpki] = publicKeySpki
            it[isCurrent] = true
            it[registeredAt] = System.currentTimeMillis()
        }
        Unit
    }

    override fun currentParentKey(childId: String): Pair<String, String>? = transaction(db) {
        ParentKeys.selectAll().where { (ParentKeys.childId eq childId) and (ParentKeys.isCurrent eq true) }
            .singleOrNull()?.let { it[ParentKeys.keyId] to it[ParentKeys.publicKeySpki] }
    }

    override fun addAlerts(childId: String, items: List<Alert>) = transaction(db) {
        Alerts.batchInsert(items) { a ->
            this[Alerts.id] = a.id
            this[Alerts.childId] = a.childId
            this[Alerts.category] = a.category.name
            this[Alerts.severity] = a.severity.name
            this[Alerts.createdAt] = a.createdAtEpochMillis
            this[Alerts.sourceApp] = a.sourceApp
            this[Alerts.contextSnippet] = a.contextSnippet
            this[Alerts.reason] = a.reason
            this[Alerts.status] = a.status.name
        }
        Unit
    }

    override fun alerts(childId: String): List<Alert> = transaction(db) {
        Alerts.selectAll().where { Alerts.childId eq childId }
            .orderBy(Alerts.createdAt to SortOrder.DESC)
            .map {
                Alert(
                    id = it[Alerts.id],
                    childId = it[Alerts.childId],
                    category = RiskCategory.valueOf(it[Alerts.category]),
                    severity = Severity.valueOf(it[Alerts.severity]),
                    createdAtEpochMillis = it[Alerts.createdAt],
                    sourceApp = it[Alerts.sourceApp],
                    contextSnippet = it[Alerts.contextSnippet],
                    reason = it[Alerts.reason],
                    status = AlertStatus.valueOf(it[Alerts.status]),
                )
            }
            .sortedByDescending { it.severity.rank }
    }

    override fun addActivity(childId: String, items: List<ActivityRecord>) = transaction(db) {
        Activities.batchInsert(items) { a ->
            this[Activities.id] = a.id
            this[Activities.childId] = a.childId
            this[Activities.type] = a.type.name
            this[Activities.createdAt] = a.createdAtEpochMillis
            this[Activities.sourceApp] = a.sourceApp
            this[Activities.content] = a.content
            this[Activities.counterparty] = a.counterparty
            this[Activities.riskCategory] = a.riskCategory?.name
            this[Activities.severity] = a.severity?.name
        }
        Unit
    }

    override fun activity(childId: String): List<ActivityRecord> = transaction(db) {
        Activities.selectAll().where { Activities.childId eq childId }
            .map {
                ActivityRecord(
                    id = it[Activities.id],
                    childId = it[Activities.childId],
                    type = ActivityType.valueOf(it[Activities.type]),
                    createdAtEpochMillis = it[Activities.createdAt],
                    sourceApp = it[Activities.sourceApp],
                    content = it[Activities.content],
                    counterparty = it[Activities.counterparty],
                    riskCategory = it[Activities.riskCategory]?.let(RiskCategory::valueOf),
                    severity = it[Activities.severity]?.let(Severity::valueOf),
                )
            }
            .sortedWith(
                compareByDescending<ActivityRecord> { it.isFlagged }
                    .thenByDescending { it.createdAtEpochMillis }
            )
    }

    override fun addMedia(media: CapturedMedia, envelope: SealedEnvelope) = transaction(db) {
        MediaItems.deleteWhere { MediaItems.id eq media.id }
        MediaItems.insert {
            it[id] = media.id
            it[childId] = media.childId
            it[kind] = media.kind.name
            it[trigger] = media.trigger.name
            it[alertId] = media.alertId
            it[mimeType] = media.mimeType
            it[capturedAt] = media.capturedAtEpochMillis
            it[keyId] = envelope.keyId
            it[wrappedKey] = envelope.wrappedKey
            it[iv] = envelope.iv
            it[ciphertext] = envelope.ciphertext
        }
        Unit
    }

    override fun mediaRefs(childId: String): List<MediaRef> = transaction(db) {
        MediaItems.selectAll().where { MediaItems.childId eq childId }
            .orderBy(MediaItems.capturedAt to SortOrder.DESC)
            .map { MediaRef(rowToMedia(it), it[MediaItems.keyId]) }
    }

    override fun media(childId: String, mediaId: String): SealedMediaResponse? = transaction(db) {
        MediaItems.selectAll().where { (MediaItems.childId eq childId) and (MediaItems.id eq mediaId) }
            .singleOrNull()?.let {
                SealedMediaResponse(
                    media = rowToMedia(it),
                    envelope = SealedEnvelope(
                        keyId = it[MediaItems.keyId],
                        wrappedKey = it[MediaItems.wrappedKey],
                        iv = it[MediaItems.iv],
                        ciphertext = it[MediaItems.ciphertext],
                    ),
                )
            }
    }

    private fun rowToMedia(it: org.jetbrains.exposed.sql.ResultRow) = CapturedMedia(
        id = it[MediaItems.id],
        childId = it[MediaItems.childId],
        kind = MediaKind.valueOf(it[MediaItems.kind]),
        trigger = MediaTrigger.valueOf(it[MediaItems.trigger]),
        alertId = it[MediaItems.alertId],
        mimeType = it[MediaItems.mimeType],
        capturedAtEpochMillis = it[MediaItems.capturedAt],
    )

    override fun recordAccess(entry: AuditEntry) = transaction(db) {
        AuditLog.insert {
            it[at] = entry.atEpochMillis
            it[actor] = entry.actor
            it[action] = entry.action.name
            it[resource] = entry.resource.name
            it[childId] = entry.childId
            it[detail] = entry.detail
        }
        Unit
    }

    override fun auditTrail(childId: String): List<AuditEntry> = transaction(db) {
        AuditLog.selectAll().where { AuditLog.childId eq childId }
            .orderBy(AuditLog.at to SortOrder.DESC)
            .map {
                AuditEntry(
                    atEpochMillis = it[AuditLog.at],
                    actor = it[AuditLog.actor],
                    action = AuditAction.valueOf(it[AuditLog.action]),
                    resource = AuditResource.valueOf(it[AuditLog.resource]),
                    childId = it[AuditLog.childId],
                    detail = it[AuditLog.detail],
                )
            }
    }

    override fun pruneOlderThan(cutoffEpochMillis: Long): Int = transaction(db) {
        val a = Alerts.deleteWhere { Alerts.createdAt less cutoffEpochMillis }
        val b = Activities.deleteWhere { Activities.createdAt less cutoffEpochMillis }
        val c = MediaItems.deleteWhere { MediaItems.capturedAt less cutoffEpochMillis }
        a + b + c
    }
}
