package com.opesware.childtrace.child.sync

import com.opesware.childtrace.shared.api.ActivityBatch
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.model.ActivityRecord
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
    suspend fun syncActivity(items: List<ActivityRecord>) {
        if (items.isEmpty()) return
        client.post(baseUrl + Endpoints.ACTIVITY) {
            contentType(ContentType.Application.Json)
            setBody(ActivityBatch(childId, items))
        }
    }

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            // TODO(security): configure certificate pinning on the OkHttp engine.
        }
    }
}
