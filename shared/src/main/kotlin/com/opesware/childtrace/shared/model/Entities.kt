package com.opesware.childtrace.shared.model

import kotlinx.serialization.Serializable

/** The buyer/account holder. One per deployment; authenticates to the Parent app. */
@Serializable
data class Parent(
    val id: String,
    val displayName: String,
    val contact: String,
)

/**
 * A monitored child profile. [age] is bound to the signed [ConsentRecord] and
 * is validated against the hard ceiling at provisioning — a profile with
 * age >= [AgeBand.ADULT_AGE] must never be created.
 */
@Serializable
data class Child(
    val id: String,
    val parentId: String,
    val displayName: String,
    val age: Int,
) {
    /** The band this child falls into, or null if the age is out of range. */
    val band: AgeBand? get() = AgeBand.fromAge(age)
}

/** The child's paired phone. */
@Serializable
data class Device(
    val id: String,
    val childId: String,
    val pairingToken: String,
    val appVersion: String,
    val captureActive: Boolean,
)

/**
 * The legal accountability anchor: the signed, in-person consent captured at
 * the sales counter. Retained for the life of the deployment. The app cannot
 * create one without the sales-process references; software only enforces the
 * age ceiling and the pairing binding.
 */
@Serializable
data class ConsentRecord(
    val id: String,
    val childId: String,
    val buyerName: String,
    /** The buyer's verified age at sale (sales gate requires >= 40). */
    val buyerAge: Int,
    /** Reference to the stored proof-of-guardianship document. */
    val guardianshipProofRef: String,
    /** Reference to the scanned signed consent certificate. */
    val signedCertificateRef: String,
    /** Provisioning timestamp (epoch millis). */
    val provisionedAtEpochMillis: Long,
    /** The child's age recorded at provisioning, bound here to prevent later bypass. */
    val childAgeAtProvisioning: Int,
)
