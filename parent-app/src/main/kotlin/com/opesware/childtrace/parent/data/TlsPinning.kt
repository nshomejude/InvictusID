package com.opesware.childtrace.parent.data

import okhttp3.CertificatePinner

/**
 * Certificate pinning for the backend connection (invariant 3, data in transit).
 * The SHA-256 SPKI pins are PLACEHOLDERS — replace with the real backend leaf
 * plus a backup pin before shipping, and keep at least two so a key rotation
 * doesn't brick the app.
 */
object TlsPinning {
    private const val PRIMARY_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val BACKUP_PIN = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="

    fun pinnerFor(host: String): CertificatePinner =
        CertificatePinner.Builder()
            .add(host, PRIMARY_PIN, BACKUP_PIN)
            .build()
}
