package com.opesware.childtrace.child.sync

import okhttp3.CertificatePinner

/**
 * Certificate pinning for the backend connection. Pinning the backend's public
 * key hashes means a mis-issued or intercepting certificate (a corporate proxy,
 * a compromised CA) is rejected — defense for data in transit (invariant 3).
 *
 * The SHA-256 SPKI pins below are PLACEHOLDERS. Before shipping, replace them
 * with the real backend leaf + a backup/intermediate pin (keep at least two so
 * a key rotation does not brick the app), e.g. obtained via:
 *   openssl s_client -connect api.childtrace.example:443 | \
 *     openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | \
 *     openssl dgst -sha256 -binary | base64
 */
object TlsPinning {
    private const val PRIMARY_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    private const val BACKUP_PIN = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="

    fun pinnerFor(host: String): CertificatePinner =
        CertificatePinner.Builder()
            .add(host, PRIMARY_PIN, BACKUP_PIN)
            .build()
}
