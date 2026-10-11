package com.opesware.childtrace.parent.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.opesware.childtrace.shared.crypto.PairingKeys
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

/**
 * The parent's device-resident RSA key pair, generated in and never leaving the
 * Android Keystore. The public key is registered to the backend at pairing so
 * the child can seal content to it; the private key — held only here — opens
 * that content. This is what makes the synced data end-to-end encrypted to the
 * parent (invariant 3): the backend only ever stores ciphertext.
 */
class ParentKeystore {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun ensureKeyPair() {
        if (keyStore.containsAlias(ALIAS)) return
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setKeySize(2048)
                .build()
        )
        generator.generateKeyPair()
    }

    fun publicKey(): PublicKey {
        ensureKeyPair()
        return keyStore.getCertificate(ALIAS).publicKey
    }

    fun privateKey(): PrivateKey {
        ensureKeyPair()
        return (keyStore.getEntry(ALIAS, null) as KeyStore.PrivateKeyEntry).privateKey
    }

    /** Base64 SPKI to register with the backend at pairing. */
    fun publicKeySpki(): String = PairingKeys.encode(publicKey())

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "childtrace.parent.rsa"
    }
}
