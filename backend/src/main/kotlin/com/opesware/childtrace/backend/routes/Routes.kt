package com.opesware.childtrace.backend.routes

import com.opesware.childtrace.backend.plugins.ProvisioningRefusedException
import com.opesware.childtrace.backend.repo.Store
import com.opesware.childtrace.shared.api.AcceptedResponse
import com.opesware.childtrace.shared.api.ActivityBatch
import com.opesware.childtrace.shared.api.AlertBatch
import com.opesware.childtrace.shared.api.ConsentRequest
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.HealthResponse
import com.opesware.childtrace.shared.api.PairRequest
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
        require(req.childId.isNotBlank()) { "childId is required" }
        require(req.pairingToken.isNotBlank()) { "pairingToken is required" }
        // A device cannot pair without a consent record on file for the child.
        store.consent(req.childId)
            ?: throw ProvisioningRefusedException("No consent record on file for child ${req.childId}.")

        val device = Device(
            id = "dev-${req.childId}",
            childId = req.childId,
            pairingToken = req.pairingToken,
            appVersion = req.appVersion,
            captureActive = true,
        )
        store.saveDevice(device)
        call.respond(HttpStatusCode.Created, device)
    }
}

/** Child device pushes flagged alerts; parent app reads the ranked feed. */
fun Route.alertRoutes(store: Store) {
    post(Endpoints.ALERTS) {
        val batch = call.receive<AlertBatch>()
        store.addAlerts(batch.childId, batch.alerts)
        call.respond(HttpStatusCode.Accepted, AcceptedResponse(batch.alerts.size))
    }
    get("/api/v1/children/{childId}/alerts") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.alerts(childId))
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
        call.respond(HttpStatusCode.Accepted, AcceptedResponse(batch.activity.size))
    }
    get("/api/v1/children/{childId}/activity") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
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
                call.respond(HttpStatusCode.Created, mapOf("band" to result.band.name, "childId" to req.childId))
            }
        }
    }
}
