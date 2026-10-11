package com.opesware.childtrace.child.sync

import com.opesware.childtrace.shared.api.ActivityBatch
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.ParentKeyResponse
import com.opesware.childtrace.shared.crypto.EnvelopeCrypto
import com.opesware.childtrace.shared.crypto.PairingKeys
import com.opesware.childtrace.shared.model.ActivityRecord
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

    private var cachedParentKey: java.security.PublicKey? = null

    private suspend fun fetchParentPublicKey(): java.security.PublicKey {
        cachedParentKey?.let { return it }
        val resp: ParentKeyResponse = client.get(baseUrl + Endpoints.parentKey(childId)).body()
        return PairingKeys.decodePublicKey(resp.publicKeySpki).also { cachedParentKey = it }
    }

    companion object {
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
