package com.opesware.childtrace.backend.routes

import com.opesware.childtrace.backend.plugins.ProvisioningRefusedException
import com.opesware.childtrace.backend.repo.Store
import com.opesware.childtrace.shared.engine.Provisioning
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.ConsentRecord
import com.opesware.childtrace.shared.model.Device
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String = "ok", val service: String = "childtrace-backend")

fun Route.healthRoutes() {
    get("/api/v1/health") { call.respond(HealthResponse()) }
}

@Serializable
data class PairRequest(val childId: String, val pairingToken: String, val appVersion: String)

/** Binds a child device to a parent account via a one-time pairing token. */
fun Route.pairingRoutes(store: Store) {
    post("/api/v1/devices/pair") {
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

@Serializable
data class AlertBatch(val childId: String, val alerts: List<Alert>)

/** Child device pushes flagged alerts; parent app reads the ranked feed. */
fun Route.alertRoutes(store: Store) {
    post("/api/v1/alerts") {
        val batch = call.receive<AlertBatch>()
        store.addAlerts(batch.childId, batch.alerts)
        call.respond(HttpStatusCode.Accepted, mapOf("accepted" to batch.alerts.size))
    }
    get("/api/v1/children/{childId}/alerts") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.alerts(childId))
    }
}

@Serializable
data class ActivityBatch(val childId: String, val activity: List<ActivityRecord>)

/**
 * Comprehensive (visible) activity sync. Content arrives already encrypted at
 * the app layer; the server stores it scoped to the one child and ranks flagged
 * items to the top for the parent.
 */
fun Route.activityRoutes(store: Store) {
    post("/api/v1/activity") {
        val batch = call.receive<ActivityBatch>()
        store.addActivity(batch.childId, batch.activity)
        call.respond(HttpStatusCode.Accepted, mapOf("accepted" to batch.activity.size))
    }
    get("/api/v1/children/{childId}/activity") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.activity(childId))
    }
}

@Serializable
data class ConsentRequest(
    val childId: String,
    val childAge: Int,
    val buyerName: String,
    val buyerAge: Int,
    val guardianshipProofRef: String,
    val signedCertificateRef: String,
)

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
    post("/api/v1/consent") {
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
