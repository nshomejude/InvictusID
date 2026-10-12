package com.opesware.childtrace.shared.api

import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.Alert
import com.opesware.childtrace.shared.model.CapturedMedia
import com.opesware.childtrace.shared.crypto.SealedEnvelope
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

/**
 * The parent registers an RSA public key (base64 SPKI) for a child. Registering
 * a new key rotates: it becomes current, and older keys are retained only as
 * historical ids (the parent keeps the matching private keys in its keyring).
 */
@Serializable
data class ParentKeyRequest(val childId: String, val keyId: String, val publicKeySpki: String)

/** The child fetches the parent's *current* public key so it can seal synced content. */
@Serializable
data class ParentKeyResponse(val childId: String, val keyId: String, val publicKeySpki: String)

// --- Pairing / onboarding flow ---

/** Agent opens a pairing session for a provisioned child; backend returns a code. */
@Serializable
data class CreatePairingRequest(val childId: String)

@Serializable
data class PairingSessionResponse(
    val code: String,
    val childId: String,
    val status: String,
    val expiresAtEpochMillis: Long,
)

/** Parent claims the session with a code and registers its key; must happen before the child pairs. */
@Serializable
data class ClaimPairingRequest(val code: String, val keyId: String, val publicKeySpki: String)

@Serializable
data class ClaimPairingResponse(val childId: String, val band: String)

@Serializable
data class AlertBatch(val childId: String, val alerts: List<Alert>)

@Serializable
data class ActivityBatch(val childId: String, val activity: List<ActivityRecord>)

@Serializable
data class AcceptedResponse(val accepted: Int)

// --- Captured media (flagged evidence + explicit screen capture) ---

/**
 * A piece of [CapturedMedia] uploaded to the backend with its bytes already
 * envelope-sealed to the parent ([envelope]). The server stores the sealed
 * blob opaquely — it cannot read it — and serves it back only to the parent
 * app, which decrypts locally. The backend re-checks the capture justification
 * against [com.opesware.childtrace.shared.engine.MediaPolicy] before accepting.
 */
@Serializable
data class MediaUploadRequest(
    val media: CapturedMedia,
    val envelope: SealedEnvelope,
)

/**
 * Metadata for one stored media item, returned when the parent lists a child's
 * media. Deliberately carries no ciphertext: the parent fetches the sealed blob
 * by id only when they open it, so listing a feed does not pull every blob.
 */
@Serializable
data class MediaRef(val media: CapturedMedia, val keyId: String)

/** The full sealed item the parent fetches to view one piece of media. */
@Serializable
data class SealedMediaResponse(val media: CapturedMedia, val envelope: SealedEnvelope)

@Serializable
data class MediaListResponse(val items: List<MediaRef>)

object Endpoints {
    const val HEALTH = "/api/v1/health"
    const val PAIR = "/api/v1/devices/pair"
    const val ALERTS = "/api/v1/alerts"
    const val ACTIVITY = "/api/v1/activity"
    const val CONSENT = "/api/v1/consent"
    const val PARENT_KEY = "/api/v1/parent-key"
    const val PAIRING_SESSIONS = "/api/v1/pairing-sessions"
    const val PAIRING_CLAIM = "/api/v1/pairing-sessions/claim"
    const val MEDIA = "/api/v1/media"
    fun childAlerts(childId: String) = "/api/v1/children/$childId/alerts"
    fun childActivity(childId: String) = "/api/v1/children/$childId/activity"
    fun parentKey(childId: String) = "/api/v1/children/$childId/parent-key"
    fun childMedia(childId: String) = "/api/v1/children/$childId/media"
    fun childMediaItem(childId: String, mediaId: String) = "/api/v1/children/$childId/media/$mediaId"
}
