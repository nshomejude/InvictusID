package com.opesware.childtrace.shared.crypto

import kotlinx.serialization.Serializable
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Envelope (hybrid) encryption used to seal content synced from the child's
 * device so that **only the paired parent** can read it — not the backend, not
 * Opesware, not anyone who obtains the stored rows.
 *
 * Each payload gets a fresh AES-256-GCM content key. The content is encrypted
 * with it; the content key is then wrapped to the parent's RSA public key
 * (OAEP). The parent, holding the matching private key, unwraps the content key
 * and decrypts. This is the end-to-end layer that invariant 3 ("encrypted in
 * transit and at rest, scoped to the parent's own child") ultimately rests on.
 *
 * Standard JCE only (RSA-OAEP-SHA256 + AES-GCM), so it runs unchanged on the
 * JVM backend tooling and on Android.
 */
object EnvelopeCrypto {

    private const val RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val AES_TRANSFORM = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_LEN = 12

    /** Seals [plaintext] to [parentPublicKey]. */
    fun seal(plaintext: ByteArray, parentPublicKey: PublicKey): SealedEnvelope {
        val contentKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        val iv = ByteArray(GCM_IV_LEN).also { SecureRandom().nextBytes(it) }
        val aes = Cipher.getInstance(AES_TRANSFORM)
        aes.init(Cipher.ENCRYPT_MODE, contentKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = aes.doFinal(plaintext)

        val rsa = Cipher.getInstance(RSA_TRANSFORM)
        rsa.init(Cipher.ENCRYPT_MODE, parentPublicKey)
        val wrappedKey = rsa.doFinal(contentKey.encoded)

        val b64 = Base64.getEncoder()
        return SealedEnvelope(
            keyId = PairingKeys.fingerprint(parentPublicKey),
            wrappedKey = b64.encodeToString(wrappedKey),
            iv = b64.encodeToString(iv),
            ciphertext = b64.encodeToString(ciphertext),
        )
    }

    /** Opens a [SealedEnvelope] with the parent's [privateKey]. */
    fun open(envelope: SealedEnvelope, privateKey: PrivateKey): ByteArray {
        val b64 = Base64.getDecoder()
        val rsa = Cipher.getInstance(RSA_TRANSFORM)
        rsa.init(Cipher.DECRYPT_MODE, privateKey)
        val contentKeyBytes = rsa.doFinal(b64.decode(envelope.wrappedKey))
        val contentKey = SecretKeySpec(contentKeyBytes, "AES")

        val aes = Cipher.getInstance(AES_TRANSFORM)
        aes.init(Cipher.DECRYPT_MODE, contentKey, GCMParameterSpec(GCM_TAG_BITS, b64.decode(envelope.iv)))
        return aes.doFinal(b64.decode(envelope.ciphertext))
    }

    fun seal(plaintext: String, parentPublicKey: PublicKey): SealedEnvelope =
        seal(plaintext.toByteArray(Charsets.UTF_8), parentPublicKey)

    fun openToString(envelope: SealedEnvelope, privateKey: PrivateKey): String =
        String(open(envelope, privateKey), Charsets.UTF_8)
}

/**
 * The sealed output: a wrapped content key plus the AES-GCM iv and ciphertext
 * (all base64). [keyId] fingerprints the parent public key this was sealed to,
 * so after a key rotation the parent can pick the matching private key from its
 * keyring to open it.
 */
@Serializable
data class SealedEnvelope(
    val keyId: String,
    val wrappedKey: String,
    val iv: String,
    val ciphertext: String,
)

/** Encoding helpers for the parent's RSA key pair, exchanged at pairing. */
object PairingKeys {
    private const val ALGO = "RSA"

    fun decodePublicKey(base64Spki: String): PublicKey {
        val spec = X509EncodedKeySpec(Base64.getDecoder().decode(base64Spki))
        return KeyFactory.getInstance(ALGO).generatePublic(spec)
    }

    fun decodePrivateKey(base64Pkcs8: String): PrivateKey {
        val spec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Pkcs8))
        return KeyFactory.getInstance(ALGO).generatePrivate(spec)
    }

    fun encode(key: java.security.Key): String = Base64.getEncoder().encodeToString(key.encoded)

    /**
     * A stable short id for a public key: base64url of the SHA-256 of its SPKI
     * encoding. Used to tag sealed envelopes and to name keyring entries, so a
     * rotated-out key's data can still be routed to the right private key.
     */
    fun fingerprint(key: PublicKey): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(key.encoded)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).take(16)
    }
}
