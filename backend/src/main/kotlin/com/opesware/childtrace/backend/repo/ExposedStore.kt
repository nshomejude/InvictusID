package com.opesware.childtrace.backend.repo

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.AlertStatus
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import com.opesware.childtrace.shared.model.RiskCategory
import com.opesware.childtrace.shared.model.Severity
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
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
            SchemaUtils.createMissingTablesAndColumns(Devices, Consents, Alerts, Activities)
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
}
