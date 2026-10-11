package com.opesware.childtrace.agent.data

import com.opesware.childtrace.shared.api.CreatePairingRequest
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.PairingSessionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * The agent's backend calls. The agent runs the human provisioning gate
 * (buyer 40+, guardianship proof, own child) offline; this opens the pairing
 * session for a child whose consent is already recorded, yielding the code the
 * parent then claims. No content ever passes through the agent.
 */
class AgentClient(
    private val baseUrl: String,
    private val client: HttpClient = defaultClient(),
) {
    suspend fun openPairingSession(childId: String): PairingSessionResponse =
        client.post(baseUrl + Endpoints.PAIRING_SESSIONS) {
            contentType(ContentType.Application.Json)
            setBody(CreatePairingRequest(childId))
        }.body()

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }
}
