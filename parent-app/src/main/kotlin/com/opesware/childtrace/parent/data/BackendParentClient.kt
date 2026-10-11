package com.opesware.childtrace.parent.data

import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
    suspend fun alerts(childId: String): List<Alert> =
        client.get(baseUrl + Endpoints.childAlerts(childId)).body()

    suspend fun activity(childId: String): List<ActivityRecord> =
        client.get(baseUrl + Endpoints.childActivity(childId)).body()

    companion object {
        fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            // TODO(security): configure certificate pinning on the OkHttp engine.
        }
    }
}
