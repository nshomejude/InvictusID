package com.opesware.childtrace.backend

import com.opesware.childtrace.backend.plugins.configureSerialization
import com.opesware.childtrace.backend.plugins.configureStatusPages
import com.opesware.childtrace.backend.repo.ExposedStore
import com.opesware.childtrace.backend.repo.InMemoryStore
import com.opesware.childtrace.backend.repo.Store
import com.opesware.childtrace.backend.routes.activityRoutes
import com.opesware.childtrace.backend.routes.alertRoutes
import com.opesware.childtrace.backend.routes.consentRoutes
import com.opesware.childtrace.backend.routes.healthRoutes
import com.opesware.childtrace.backend.routes.mediaRoutes
import com.opesware.childtrace.backend.routes.pairingFlowRoutes
import com.opesware.childtrace.backend.routes.pairingRoutes
import com.opesware.childtrace.backend.routes.parentKeyRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

/**
 * The Opesware backend does three jobs: pair parent and child devices, carry
 * flagged alerts and (under the comprehensive-monitoring model) encrypted
 * activity, and hold the signed ConsentRecord that makes each deployment
 * accountable. It holds as little as the model allows and never resells data.
 *
 * The store is chosen from the environment: set DATABASE_URL (plus DATABASE_USER
 * / DATABASE_PASSWORD) to use PostgreSQL via [ExposedStore]; otherwise it falls
 * back to the in-memory store for local development and tests.
 */
fun Application.module() {
    val store: Store = System.getenv("DATABASE_URL")?.let { url ->
        ExposedStore(
            jdbcUrl = url,
            user = System.getenv("DATABASE_USER") ?: "childtrace",
            password = System.getenv("DATABASE_PASSWORD") ?: "",
        )
    } ?: InMemoryStore()

    install(DefaultHeaders)
    install(CallLogging)
    configureSerialization()
    configureStatusPages()

    // Bounded retention: expire alerts/activity past the window (invariant 3).
    RetentionScheduler(store).start()

    // The 16–17 band is refused unless explicitly enabled (counsel sign-off).
    val provisioningConfig = com.opesware.childtrace.shared.engine.Provisioning.Config(
        allowSeniorTeen = System.getenv("CHILDTRACE_ALLOW_16_17") == "true",
    )

    // Explicit (visible) screen capture is refused unless enabled (counsel sign-off).
    // Flagged-media capture does not depend on this flag.
    val mediaConfig = com.opesware.childtrace.shared.engine.MediaPolicy.Config(
        allowScreenCapture = System.getenv("CHILDTRACE_ALLOW_SCREEN_CAPTURE") == "true",
    )

    routing {
        healthRoutes()
        pairingRoutes(store)
        pairingFlowRoutes(store, provisioningConfig)
        parentKeyRoutes(store)
        alertRoutes(store)
        activityRoutes(store)
        mediaRoutes(store, mediaConfig)
        consentRoutes(store, provisioningConfig)
    }
}
