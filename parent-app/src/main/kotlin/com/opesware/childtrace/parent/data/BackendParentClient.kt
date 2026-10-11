package com.opesware.childtrace.parent.data

import com.opesware.childtrace.shared.api.ClaimPairingRequest
import com.opesware.childtrace.shared.api.ClaimPairingResponse
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.ParentKeyRequest
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Pulls the ranked alert feed and activity for a child from the backend over
 * TLS (certificate pinning configured on the engine in production).
 */
class BackendParentClient(
    private val baseUrl: String,
    private val client: HttpClient = defaultClient(),
) {
    /**
     * Claims a pairing session with the agent-issued code, registering this
     * parent's current key in the same step. Returns the child id (implied by
     * the code) and the resolved policy band.
     */
    suspend fun claimPairing(code: String, keyId: String, publicKeySpki: String): ClaimPairingResponse =
        client.post(baseUrl + Endpoints.PAIRING_CLAIM) {
            contentType(ContentType.Application.Json)
            setBody(ClaimPairingRequest(code, keyId, publicKeySpki))
        }.body()

    /** Registers (or rotates to) the parent's public key for a child. */
    suspend fun registerParentKey(childId: String, keyId: String, publicKeySpki: String) {
        client.post(baseUrl + Endpoints.PARENT_KEY) {
            contentType(ContentType.Application.Json)
            setBody(ParentKeyRequest(childId, keyId, publicKeySpki))
        }
    }

    suspend fun alerts(childId: String): List<Alert> =
        client.get(baseUrl + Endpoints.childAlerts(childId)).body()

    suspend fun activity(childId: String): List<ActivityRecord> =
        client.get(baseUrl + Endpoints.childActivity(childId)).body()

    companion object {
        fun defaultClient(host: String = "api.childtrace.example"): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                config { certificatePinner(TlsPinning.pinnerFor(host)) }
            }
        }
    }
}
