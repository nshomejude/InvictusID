package com.opesware.childtrace.backend.routes

import com.opesware.childtrace.backend.push.PushDispatcher
import com.opesware.childtrace.backend.repo.Store
import com.opesware.childtrace.shared.api.ChildSummary
import com.opesware.childtrace.shared.api.Endpoints
import com.opesware.childtrace.shared.api.LocationRequest
import com.opesware.childtrace.shared.api.PushTokenRequest
import com.opesware.childtrace.shared.api.TamperRequest
import com.opesware.childtrace.shared.engine.DigestBuilder
import com.opesware.childtrace.shared.model.Geofence
import com.opesware.childtrace.shared.model.GeofenceEvent
import com.opesware.childtrace.shared.model.Geofencing
import com.opesware.childtrace.shared.model.LocationSample
import com.opesware.childtrace.shared.model.TamperEvent
import com.opesware.childtrace.shared.model.TamperKind
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/** Multi-child: list a parent's child profiles for the switcher. */
fun Route.childrenRoutes(store: Store) {
    get("/api/v1/parents/{parentId}/children") {
        val parentId = call.parameters["parentId"] ?: throw IllegalArgumentException("parentId required")
        val summaries = store.childrenOf(parentId).map {
            ChildSummary(it.id, it.displayName, it.band?.name ?: "UNKNOWN")
        }
        call.respond(summaries)
    }
}

/** Register the parent's FCM token for a child's critical pushes. */
fun Route.pushTokenRoutes(store: Store) {
    post(Endpoints.PUSH_TOKEN) {
        val req = call.receive<PushTokenRequest>()
        require(req.token.isNotBlank()) { "token is required" }
        store.savePushToken(req.parentId, req.childId, req.token)
        call.respond(HttpStatusCode.Created, mapOf("registered" to true))
    }
}

/**
 * Location ingest + geofence evaluation + history. Each sample is stored
 * (retention-bounded) and compared against the child's fences; a transition
 * becomes a reviewable geofence event for the parent.
 */
fun Route.locationRoutes(store: Store) {
    post(Endpoints.LOCATION) {
        val req = call.receive<LocationRequest>()
        val previous = store.lastLocation(req.childId)
        val sample = LocationSample(req.childId, req.latitude, req.longitude, req.atEpochMillis, req.accuracyMeters)
        store.addLocation(sample)
        val events = store.geofences(req.childId).mapNotNull { fence ->
            Geofencing.evaluate(fence, previous, sample)?.let { fence.label to it }
        }
        call.respond(HttpStatusCode.Accepted, events.map { GeofenceCrossing(it.first, it.second) })
    }
    get("/api/v1/children/{childId}/locations") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.locations(childId))
    }
    post("/api/v1/children/{childId}/geofences") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        val fence = call.receive<Geofence>()
        require(fence.childId == childId) { "childId mismatch" }
        store.saveGeofence(fence)
        call.respond(HttpStatusCode.Created, fence)
    }
    get("/api/v1/children/{childId}/geofences") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.geofences(childId))
    }
}

@kotlinx.serialization.Serializable
data class GeofenceCrossing(val fenceLabel: String, val event: GeofenceEvent)

/** Visible tamper reports from the child device; surfaced to the parent. */
fun Route.tamperRoutes(store: Store) {
    post(Endpoints.TAMPER) {
        val req = call.receive<TamperRequest>()
        store.addTamper(TamperEvent(req.childId, TamperKind.valueOf(req.kind), req.atEpochMillis, req.detail))
        call.respond(HttpStatusCode.Accepted, mapOf("recorded" to true))
    }
    get("/api/v1/children/{childId}/tamper") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        call.respond(store.tamperEvents(childId))
    }
}

/** Weekly digest: aggregate the last 7 days of alerts for a calm summary. */
fun Route.digestRoutes(store: Store) {
    get("/api/v1/children/{childId}/digest") {
        val childId = call.parameters["childId"] ?: throw IllegalArgumentException("childId required")
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        call.respond(DigestBuilder.build(childId, store.alerts(childId), now - weekMs, now))
    }
}
