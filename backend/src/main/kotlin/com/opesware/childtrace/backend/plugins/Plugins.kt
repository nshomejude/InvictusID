package com.opesware.childtrace.backend.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = false })
    }
}

/** A serializable error envelope so clients get a consistent shape. */
@kotlinx.serialization.Serializable
data class ApiError(val error: String, val detail: String? = null)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("bad_request", cause.message))
        }
        exception<ProvisioningRefusedException> { call, cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, ApiError("provisioning_refused", cause.message))
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal_error"))
        }
    }
}

/** Thrown when the age ceiling or another provisioning rule rejects a request. */
class ProvisioningRefusedException(message: String) : RuntimeException(message)
