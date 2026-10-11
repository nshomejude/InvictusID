package com.opesware.childtrace.backend.routes

import com.opesware.childtrace.backend.plugins.ProvisioningRefusedException
import com.opesware.childtrace.backend.repo.PairingSession
import com.opesware.childtrace.backend.repo.PairingStatus
import com.opesware.childtrace.backend.repo.Store
import com.opesware.childtrace.shared.api.ClaimPairingRequest
import com.opesware.childtrace.shared.api.ClaimPairingResponse
import com.opesware.childtrace.shared.api.CreatePairingRequest
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.PairingSessionResponse
import com.opesware.childtrace.shared.engine.Provisioning
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.security.SecureRandom

private const val SESSION_TTL_MS = 15 * 60 * 1000L
private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789" // no ambiguous chars
private const val CODE_LEN = 8

private fun newCode(): String {
    val rnd = SecureRandom()
    return (1..CODE_LEN).map { CODE_ALPHABET[rnd.nextInt(CODE_ALPHABET.length)] }.joinToString("")
}

/**
 * The onboarding sequence. An agent opens a session for a provisioned child;
 * the parent claims it (registering its key); only then can the child device
 * pair. Each step checks consent and ordering, and the code expires.
 */
fun Route.pairingFlowRoutes(store: Store, config: Provisioning.Config = Provisioning.Config()) {

    // Agent: open a pairing session. Requires a consent record already on file.
    post(Endpoints.PAIRING_SESSIONS) {
        val req = call.receive<CreatePairingRequest>()
        store.consent(req.childId)
            ?: throw ProvisioningRefusedException("No consent record on file for child ${req.childId}.")
        val now = System.currentTimeMillis()
        val session = PairingSession(
            code = newCode(),
            childId = req.childId,
            status = PairingStatus.PENDING,
            createdAtEpochMillis = now,
            expiresAtEpochMillis = now + SESSION_TTL_MS,
        )
        store.savePairingSession(session)
        call.respond(HttpStatusCode.Created, session.toResponse())
    }

    // Parent: claim the session with the code and register the device key.
    post(Endpoints.PAIRING_CLAIM) {
        val req = call.receive<ClaimPairingRequest>()
        val session = activeSession(store, req.code)
        if (session.status == PairingStatus.COMPLETED) {
            throw ProvisioningRefusedException("This pairing session is already complete.")
        }
        store.saveParentKey(session.childId, req.keyId, req.publicKeySpki)
        store.savePairingSession(session.copy(status = PairingStatus.PARENT_CLAIMED))

        val consent = store.consent(session.childId)
            ?: throw ProvisioningRefusedException("No consent record on file for child ${session.childId}.")
        val band = when (val r = Provisioning.validateChildAge(consent.childAgeAtProvisioning, config)) {
            is Provisioning.Result.Allowed -> r.band.name
            is Provisioning.Result.Refused -> throw ProvisioningRefusedException(r.reason)
        }
        call.respond(ClaimPairingResponse(session.childId, band))
    }
}

/** Returns a live, unexpired session for [code], or refuses. */
internal fun activeSession(store: Store, code: String): PairingSession {
    val session = store.pairingSession(code)
        ?: throw ProvisioningRefusedException("Unknown pairing code.")
    if (session.isExpired(System.currentTimeMillis())) {
        throw ProvisioningRefusedException("This pairing code has expired.")
    }
    return session
}

private fun PairingSession.toResponse() =
    PairingSessionResponse(code, childId, status.name, expiresAtEpochMillis)
