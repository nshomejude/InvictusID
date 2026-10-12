package com.opesware.childtrace.child.sync

import com.opesware.childtrace.shared.api.ActivityBatch
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.LocationRequest
import com.opesware.childtrace.shared.api.PairRequest
import com.opesware.childtrace.shared.api.ParentKeyResponse
import com.opesware.childtrace.shared.api.TamperRequest
import com.opesware.childtrace.shared.crypto.EnvelopeCrypto
import com.opesware.childtrace.shared.crypto.PairingKeys
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Device
import com.opesware.childtrace.shared.model.TamperEvent
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.json.Json as JsonFormat
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Uploads captured activity to the Opesware backend. TLS with certificate
 * pinning is configured on the OkHttp engine in production; content in each
 * [ActivityRecord] is encrypted at the application layer before it reaches here.
 */
class BackendClient(
    private val baseUrl: String,
    private val childId: String,
    private val client: HttpClient = defaultClient(),
) {
    /**
     * Seals each record's content to the parent's public key (fetched once and
     * cached), so only the paired parent can read it — the backend stores
     * ciphertext. Risk metadata (category/severity) stays in clear so the server
     * can rank danger without reading content.
     */
    suspend fun syncActivity(items: List<ActivityRecord>) {
        if (items.isEmpty()) return
        val parentKey = fetchParentPublicKey()
        val sealed = items.map { record ->
            val envelope = EnvelopeCrypto.seal(record.content, parentKey)
            record.copy(
                content = JsonFormat.encodeToString(com.opesware.childtrace.shared.crypto.SealedEnvelope.serializer(), envelope),
                counterparty = record.counterparty?.let {
                    JsonFormat.encodeToString(
                        com.opesware.childtrace.shared.crypto.SealedEnvelope.serializer(),
                        EnvelopeCrypto.seal(it, parentKey),
                    )
                },
            )
        }
        client.post(baseUrl + Endpoints.ACTIVITY) {
            contentType(ContentType.Application.Json)
            setBody(ActivityBatch(childId, sealed))
        }
    }

    /** Reports visible tamper events (disabled services, revoked permissions) to the parent. */
    suspend fun reportTamper(events: List<TamperEvent>) {
        events.forEach { e ->
            client.post(baseUrl + Endpoints.TAMPER) {
                contentType(ContentType.Application.Json)
                setBody(TamperRequest(e.childId, e.kind.name, e.atEpochMillis, e.detail))
            }
        }
    }

    /** Posts a location sample; the backend evaluates geofences and stores history. */
    suspend fun reportLocation(latitude: Double, longitude: Double, atEpochMillis: Long, accuracyMeters: Double? = null) {
        client.post(baseUrl + Endpoints.LOCATION) {
            contentType(ContentType.Application.Json)
            setBody(LocationRequest(childId, latitude, longitude, atEpochMillis, accuracyMeters))
        }
    }

    private var cachedParentKey: java.security.PublicKey? = null

    private suspend fun fetchParentPublicKey(): java.security.PublicKey {
        cachedParentKey?.let { return it }
        val resp: ParentKeyResponse = client.get(baseUrl + Endpoints.parentKey(childId)).body()
        return PairingKeys.decodePublicKey(resp.publicKeySpki).also { cachedParentKey = it }
    }

    companion object {
        /**
         * Pairs this device using the agent-issued code. The backend derives the
         * child id from the (parent-claimed) session, so the returned [Device]
         * tells the child which id it has been bound to.
         */
        suspend fun pairDevice(baseUrl: String, code: String, appVersion: String): Device {
            val client = defaultClient()
            return client.post(baseUrl + Endpoints.PAIR) {
                contentType(ContentType.Application.Json)
                setBody(PairRequest(childId = "", pairingToken = code, appVersion = appVersion))
            }.body()
        }

        fun defaultClient(host: String = "api.childtrace.example"): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            engine {
                // Pin the backend's leaf/intermediate keys so a mis-issued or
                // intercepting certificate is rejected (invariant 3, in transit).
                config { certificatePinner(TlsPinning.pinnerFor(host)) }
            }
        }
    }
}
