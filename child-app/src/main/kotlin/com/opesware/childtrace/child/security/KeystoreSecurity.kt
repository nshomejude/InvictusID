package com.opesware.childtrace.child.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Device-local cryptography anchored in the Android Keystore. The symmetric key
 * never leaves the secure hardware (StrongBox/TEE where available); we only ever
 * hand out ciphertext.
 *
 * Two uses:
 *  - [databasePassphrase] derives the SQLCipher passphrase for the local Room
 *    store, so the captured-activity database is encrypted at rest.
 *  - [encrypt]/[decrypt] provide AES-GCM for individual fields held on-device.
 *
 * NOTE ON SYNC: content that is synced to the *parent* must be readable by the
 * parent, so it is sealed with envelope encryption to the parent's key at
 * pairing time — not with this device-local key. That envelope layer is a
 * separate component (see PayloadCrypto); this class secures on-device state.
 */
class KeystoreSecurity(private val context: Context) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** Encrypts [plaintext]; returns base64(iv || ciphertext). */
    fun encrypt(plaintext: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext)
        return Base64.encodeToString(iv + ct, Base64.NO_WRAP)
    }

    /** Reverses [encrypt]. */
    fun decrypt(encoded: String): ByteArray {
        val blob = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val ct = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    /**
     * A stable passphrase for SQLCipher, derived once and stored sealed by the
     * Keystore key in app-private storage.
     */
    fun databasePassphrase(): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(PASSPHRASE_KEY, null)?.let { return decrypt(it) }
        val fresh = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        prefs.edit().putString(PASSPHRASE_KEY, encrypt(fresh)).apply()
        return fresh
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "childtrace.local.aes"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
        const val PREFS = "childtrace.secure"
        const val PASSPHRASE_KEY = "db_passphrase"
    }
}
