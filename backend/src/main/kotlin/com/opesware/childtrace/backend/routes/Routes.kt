package com.opesware.childtrace.backend.routes

import com.opesware.childtrace.backend.plugins.ProvisioningRefusedException
import com.opesware.childtrace.backend.repo.AuditAction
import com.opesware.childtrace.backend.repo.AuditEntry
import com.opesware.childtrace.backend.repo.AuditResource
import com.opesware.childtrace.backend.repo.Store
import io.ktor.server.application.ApplicationCall
import com.opesware.childtrace.shared.api.AcceptedResponse
import com.opesware.childtrace.shared.api.ActivityBatch
import com.opesware.childtrace.shared.api.AlertBatch
import com.opesware.childtrace.shared.api.ConsentRequest
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.HealthResponse
import com.opesware.childtrace.shared.api.PairRequest
import com.opesware.childtrace.shared.api.ParentKeyRequest
import com.opesware.childtrace.shared.api.ParentKeyResponse
import com.opesware.childtrace.shared.engine.Provisioning
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.healthRoutes() {
    get(Endpoints.HEALTH) { call.respond(HealthResponse()) }
}

/** Binds a child device to a parent account via a one-time pairing token. */
fun Route.pairingRoutes(store: Store) {
    post(Endpoints.PAIR) {
        val req = call.receive<PairRequest>()
        require(req.pairingToken.isNotBlank()) { "pairingToken is required" }

        // The pairing token is the session code. The child can only pair once
        // the parent has claimed the session (so a parent key exists to seal to)
        // and before it expires. This enforces the onboarding order.
        val session = activeSession(store, req.pairingToken)
        if (session.status == com.opesware.childtrace.backend.repo.PairingStatus.PENDING) {
            throw ProvisioningRefusedException("The parent must complete setup before the child device can pair.")
        }
        store.consent(session.childId)
            ?: throw ProvisioningRefusedException("No consent record on file for child ${session.childId}.")

        val device = Device(
            id = "dev-${session.childId}",
            childId = session.childId,
            pairingToken = req.pairingToken,
            appVersion = req.appVersion,
            captureActive = true,
        )
        store.saveDevice(device)
        store.savePairingSession(session.copy(status = com.opesware.childtrace.backend.repo.PairingStatus.COMPLETED))
        call.respond(HttpStatusCode.Created, device)
    }
}

/** The actor responsible for a request, for the audit trail. */
private fun ApplicationCall.actor(): String = request.headers["X-Actor"] ?: "unknown"

/** Child device pushes flagged alerts; parent app reads the ranked feed. */
fun Route.alertRoutes(store: Store) {
    post(Endpoints.ALERTS) {
        val batch = call.receive<AlertBatch>()
        store.addAlerts(batch.childId, batch.alerts)
        store.recordAccess(audit(call, AuditAction.WRITE, AuditResource.ALERT, batch.childId, "${batch.alerts.size} alerts"))
        call.respond(HttpStatusCode.Accepted, AcceptedResponse(batch.alerts.size))
    }
    get("/api/v1/children/{childId}/alerts") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        store.recordAccess(audit(call, AuditAction.READ, AuditResource.ALERT, childId))
        call.respond(store.alerts(childId))
    }
}

private fun audit(call: ApplicationCall, action: AuditAction, resource: AuditResource, childId: String, detail: String? = null) =
    AuditEntry(System.currentTimeMillis(), call.actor(), action, resource, childId, detail)

/**
 * Pairing key exchange. The parent registers their RSA public key for a child;
 * the child fetches it so it can envelope-seal synced content to the parent.
 * Only the public key is ever handled here — the private key never leaves the
 * parent's device.
 */
fun Route.parentKeyRoutes(store: Store) {
    post(Endpoints.PARENT_KEY) {
        val req = call.receive<ParentKeyRequest>()
        require(req.childId.isNotBlank()) { "childId is required" }
        require(req.keyId.isNotBlank()) { "keyId is required" }
        require(req.publicKeySpki.isNotBlank()) { "publicKeySpki is required" }
        store.saveParentKey(req.childId, req.keyId, req.publicKeySpki)
        call.respond(HttpStatusCode.Created, ParentKeyResponse(req.childId, req.keyId, req.publicKeySpki))
    }
    get("/api/v1/children/{childId}/parent-key") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        val (keyId, spki) = store.currentParentKey(childId)
            ?: throw ProvisioningRefusedException("No parent key registered for child $childId.")
        call.respond(ParentKeyResponse(childId, keyId, spki))
    }
}

/**
 * Comprehensive (visible) activity sync. Content arrives already encrypted at
 * the app layer; the server stores it scoped to the one child and ranks flagged
 * items to the top for the parent.
 */
fun Route.activityRoutes(store: Store) {
    post(Endpoints.ACTIVITY) {
        val batch = call.receive<ActivityBatch>()
        store.addActivity(batch.childId, batch.activity)
        store.recordAccess(audit(call, AuditAction.WRITE, AuditResource.ACTIVITY, batch.childId, "${batch.activity.size} records"))
        call.respond(HttpStatusCode.Accepted, AcceptedResponse(batch.activity.size))
    }
    get("/api/v1/children/{childId}/activity") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        store.recordAccess(audit(call, AuditAction.READ, AuditResource.ACTIVITY, childId))
        call.respond(store.activity(childId))
    }
}

/**
 * Records the signed consent captured in the sales application. Enforces the
 * software half of the gate via the shared [Provisioning] guard.
 *
 * NOTE: the guard currently refuses ages 16+ (the original hard ceiling). The
 * product direction extends scope to under-18, but the 16–17 band stays
 * refused in code until its policy band is defined AND Cameroon counsel signs
 * off (CLAUDE.md invariant 5/7). Raising the ceiling is a deliberate, reviewed
 * change — not a constant to flip quietly here.
 */
fun Route.consentRoutes(store: Store) {
    post(Endpoints.CONSENT) {
        val req = call.receive<ConsentRequest>()
        when (val result = Provisioning.validateChildAge(req.childAge)) {
            is Provisioning.Result.Refused -> throw ProvisioningRefusedException(result.reason)
            is Provisioning.Result.Allowed -> {
                val record = ConsentRecord(
                    id = "consent-${req.childId}",
                    childId = req.childId,
                    buyerName = req.buyerName,
                    buyerAge = req.buyerAge,
                    guardianshipProofRef = req.guardianshipProofRef,
                    signedCertificateRef = req.signedCertificateRef,
                    provisionedAtEpochMillis = System.currentTimeMillis(),
                    childAgeAtProvisioning = req.childAge,
                )
                store.saveConsent(record)
                store.recordAccess(audit(call, AuditAction.WRITE, AuditResource.CONSENT, req.childId))
                call.respond(HttpStatusCode.Created, mapOf("band" to result.band.name, "childId" to req.childId))
            }
        }
    }
}
