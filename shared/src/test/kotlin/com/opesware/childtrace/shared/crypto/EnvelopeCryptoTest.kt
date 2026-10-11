package com.opesware.childtrace.shared.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator

class EnvelopeCryptoTest {

    private fun keyPair() = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    @Test
    fun `round-trips content through the parent key pair`() {
        val kp = keyPair()
        val message = "you're so mature… let's talk on snapchat — our secret"

        val sealed = EnvelopeCrypto.seal(message, kp.public)
        val opened = EnvelopeCrypto.openToString(sealed, kp.private)

        assertEquals(message, opened)
    }

    @Test
    fun `a different private key cannot open the envelope`() {
        val a = keyPair()
        val b = keyPair()
        val sealed = EnvelopeCrypto.seal("sensitive", a.public)

        // Unwrapping the content key with the wrong RSA key fails.
        assertThrows(Exception::class.java) { EnvelopeCrypto.open(sealed, b.private) }
    }

    @Test
    fun `each seal uses a fresh content key and iv`() {
        val kp = keyPair()
        val s1 = EnvelopeCrypto.seal("same text", kp.public)
        val s2 = EnvelopeCrypto.seal("same text", kp.public)

        assertNotEquals(s1.ciphertext, s2.ciphertext)
        assertNotEquals(s1.iv, s2.iv)
        assertNotEquals(s1.wrappedKey, s2.wrappedKey)
    }

    @Test
    fun `public key survives encode-decode round trip`() {
        val kp = keyPair()
        val encoded = PairingKeys.encode(kp.public)
        val decoded = PairingKeys.decodePublicKey(encoded)

        val sealed = EnvelopeCrypto.seal("hello".toByteArray(), decoded)
        assertArrayEquals("hello".toByteArray(), EnvelopeCrypto.open(sealed, kp.private))
    }
}
