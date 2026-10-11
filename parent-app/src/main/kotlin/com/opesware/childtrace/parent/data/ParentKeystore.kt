package com.opesware.childtrace.parent.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.opesware.childtrace.shared.crypto.PairingKeys
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID

/**
 * The parent's keyring: one or more RSA key pairs in the Android Keystore
 * (private keys never leave the device). Each key is identified by its
 * fingerprint ([keyId]); sealed content carries the keyId it was sealed to, so
 * even after a rotation the parent opens old content with the matching retained
 * key.
 *
 * Rotation ([rotate]) generates a new current key and keeps the old ones. If an
 * old key is genuinely lost (device lost, keystore wiped), content sealed to it
 * is unrecoverable — the correct security property, not a bug: no one, Opesware
 * included, can recover it.
 */
class ParentKeystore(private val context: Context) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The current key's id, creating an initial key pair if none exists. */
    fun currentKeyId(): String = prefs.getString(CURRENT, null) ?: rotate()

    fun currentPublicKeySpki(): String = PairingKeys.encode(publicKey(currentKeyId()))

    /** Generates a fresh current key pair and returns its keyId. Old keys are retained. */
    fun rotate(): String {
        val alias = "$ALIAS_PREFIX${UUID.randomUUID()}"
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .build()
        )
        val pair = generator.generateKeyPair()
        val keyId = PairingKeys.fingerprint(pair.public)
        prefs.edit()
            .putString(aliasKey(keyId), alias)
            .putString(CURRENT, keyId)
            .apply()
        return keyId
    }

    fun publicKey(keyId: String): PublicKey = keyStore.getCertificate(aliasFor(keyId)).publicKey

    /** The private key for [keyId], or null if that key is not on this device. */
    fun privateKeyOrNull(keyId: String): PrivateKey? {
        val alias = prefs.getString(aliasKey(keyId), null) ?: return null
        return (keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry)?.privateKey
    }

    private fun aliasFor(keyId: String): String =
        prefs.getString(aliasKey(keyId), null) ?: error("No keystore alias for keyId $keyId")

    private fun aliasKey(keyId: String) = "alias.$keyId"

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_PREFIX = "childtrace.parent.rsa."
        const val PREFS = "childtrace.parent.keys"
        const val CURRENT = "current_key_id"
    }
}
