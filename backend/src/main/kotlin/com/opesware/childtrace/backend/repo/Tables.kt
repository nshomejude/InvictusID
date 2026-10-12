package com.opesware.childtrace.backend.repo

import org.jetbrains.exposed.sql.Table

/**
 * Relational schema. The backend holds as little as the model allows: devices,
 * consent records, flagged alerts, and (under comprehensive monitoring) synced
 * activity — all scoped to a child.
 *
 * Content fields (alert context, activity content) arrive already encrypted at
 * the application layer; they are stored as opaque text and are the most
 * sensitive columns in the system. Every access is expected to be audit-logged
 * at the service layer.
 */
object Devices : Table("devices") {
    val id = varchar("id", 64)
    val childId = varchar("child_id", 64).index()
    val pairingToken = varchar("pairing_token", 128)
    val appVersion = varchar("app_version", 32)
    val captureActive = bool("capture_active")
    override val primaryKey = PrimaryKey(id)
}

object Consents : Table("consents") {
    val id = varchar("id", 64)
    val childId = varchar("child_id", 64).uniqueIndex()
    val buyerName = varchar("buyer_name", 256)
    val buyerAge = integer("buyer_age")
    val guardianshipProofRef = varchar("guardianship_proof_ref", 256)
    val signedCertificateRef = varchar("signed_certificate_ref", 256)
    val provisionedAt = long("provisioned_at")
    val childAgeAtProvisioning = integer("child_age_at_provisioning")
    override val primaryKey = PrimaryKey(id)
}

object Alerts : Table("alerts") {
    val id = varchar("id", 64)
    val childId = varchar("child_id", 64).index()
    val category = varchar("category", 48)
    val severity = varchar("severity", 16)
    val createdAt = long("created_at")
    val sourceApp = varchar("source_app", 128)
    val contextSnippet = text("context_snippet") // encrypted at app layer
    val reason = text("reason")
    val status = varchar("status", 16)
    override val primaryKey = PrimaryKey(id)
}

object Activities : Table("activities") {
    val id = varchar("id", 64)
    val childId = varchar("child_id", 64).index()
    val type = varchar("type", 24)
    val createdAt = long("created_at")
    val sourceApp = varchar("source_app", 128).nullable()
    val content = text("content") // encrypted at app layer
    val counterparty = varchar("counterparty", 256).nullable()
    val riskCategory = varchar("risk_category", 48).nullable()
    val severity = varchar("severity", 16).nullable()
    override val primaryKey = PrimaryKey(id)
}

object PairingSessions : Table("pairing_sessions") {
    val code = varchar("code", 32)
    val childId = varchar("child_id", 64).index()
    val status = varchar("status", 16)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
    override val primaryKey = PrimaryKey(code)
}

object ParentKeys : Table("parent_keys") {
    val childId = varchar("child_id", 64).index()
    val keyId = varchar("key_id", 32)
    val publicKeySpki = text("public_key_spki")
    val isCurrent = bool("is_current")
    val registeredAt = long("registered_at")
    // One row per (child, key) so rotations keep the history of key ids.
    override val primaryKey = PrimaryKey(childId, keyId)
}

object MediaItems : Table("media_items") {
    val id = varchar("id", 64)
    val childId = varchar("child_id", 64).index()
    val kind = varchar("kind", 16)
    val trigger = varchar("trigger", 32)
    val alertId = varchar("alert_id", 64).nullable()
    val mimeType = varchar("mime_type", 64)
    val capturedAt = long("captured_at")
    // Envelope-sealed to the parent key; opaque to the server.
    val keyId = varchar("key_id", 32)
    val wrappedKey = text("wrapped_key")
    val iv = varchar("iv", 64)
    val ciphertext = text("ciphertext")
    override val primaryKey = PrimaryKey(id)
}

object AuditLog : Table("audit_log") {
    val id = long("id").autoIncrement()
    val at = long("at")
    val actor = varchar("actor", 128)
    val action = varchar("action", 8)
    val resource = varchar("resource", 16)
    val childId = varchar("child_id", 64).index()
    val detail = varchar("detail", 512).nullable()
    override val primaryKey = PrimaryKey(id)
}
