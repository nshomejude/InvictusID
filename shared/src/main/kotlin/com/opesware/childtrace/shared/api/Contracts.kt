package com.opesware.childtrace.shared.api

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import kotlinx.serialization.Serializable

/**
 * The wire contract shared by the backend and both apps, so request/response
 * shapes stay in lockstep. Content fields inside [Alert]/[ActivityRecord] are
 * encrypted at the application layer before they are placed in these payloads.
 */

@Serializable
data class HealthResponse(val status: String = "ok", val service: String = "childtrace-backend")

@Serializable
data class PairRequest(val childId: String, val pairingToken: String, val appVersion: String)

@Serializable
data class ConsentRequest(
    val childId: String,
    val childAge: Int,
    val buyerName: String,
    val buyerAge: Int,
    val guardianshipProofRef: String,
    val signedCertificateRef: String,
)

@Serializable
data class AlertBatch(val childId: String, val alerts: List<Alert>)

@Serializable
data class ActivityBatch(val childId: String, val activity: List<ActivityRecord>)

@Serializable
data class AcceptedResponse(val accepted: Int)

object Endpoints {
    const val HEALTH = "/api/v1/health"
    const val PAIR = "/api/v1/devices/pair"
    const val ALERTS = "/api/v1/alerts"
    const val ACTIVITY = "/api/v1/activity"
    const val CONSENT = "/api/v1/consent"
    fun childAlerts(childId: String) = "/api/v1/children/$childId/alerts"
    fun childActivity(childId: String) = "/api/v1/children/$childId/activity"
}
