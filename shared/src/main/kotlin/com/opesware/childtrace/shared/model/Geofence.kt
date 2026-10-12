package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A named circular safe-zone the parent sets for a child (home, school). */
@Serializable
data class Geofence(
    val id: String,
    val childId: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    /** Alert when the child enters, leaves, or both. */
    val trigger: GeofenceTrigger = GeofenceTrigger.BOTH,
)

@Serializable
enum class GeofenceTrigger { ENTER, EXIT, BOTH }

/** A location sample from the child device (visible to the child; retained bounded). */
@Serializable
data class LocationSample(
    val childId: String,
    val latitude: Double,
    val longitude: Double,
    val atEpochMillis: Long,
    val accuracyMeters: Double? = null,
)

/** The transition a sample represents relative to a fence, if any. */
@Serializable
enum class GeofenceEvent { ENTERED, EXITED }

object Geofencing {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance between two points, in meters (haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    fun isInside(fence: Geofence, lat: Double, lon: Double): Boolean =
        distanceMeters(fence.latitude, fence.longitude, lat, lon) <= fence.radiusMeters

    /**
     * Given the child's previous and current position, returns the geofence
     * event to raise for [fence] (null if no reportable transition). [previous]
     * null means first fix — no transition is inferred.
     */
    fun evaluate(fence: Geofence, previous: LocationSample?, current: LocationSample): GeofenceEvent? {
        val nowInside = isInside(fence, current.latitude, current.longitude)
        val wasInside = previous?.let { isInside(fence, it.latitude, it.longitude) }
        return when {
            wasInside == null -> null
            !wasInside && nowInside && fence.trigger != GeofenceTrigger.EXIT -> GeofenceEvent.ENTERED
            wasInside && !nowInside && fence.trigger != GeofenceTrigger.ENTER -> GeofenceEvent.EXITED
            else -> null
        }
    }
}
