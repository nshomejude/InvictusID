package com.opesware.childtrace.shared

import com.opesware.childtrace.shared.crypto.EnvelopeCrypto
import com.opesware.childtrace.shared.crypto.SealedEnvelope
import com.opesware.childtrace.shared.model.ActivityRecord
import com.opesware.childtrace.shared.model.ActivityType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator

/**
 * Exercises the whole sync path with the real serialized shapes, the way the
 * apps use it: the child seals a record's content to the parent's public key
 * and serializes the envelope into the ActivityRecord it syncs; the backend
 * only ever sees that ciphertext; the parent deserializes and opens it.
 *
 * This is the cross-cutting guarantee behind invariant 3 (only the paired
 * parent can read synced content), verified on the JVM.
 */
class EndToEndSyncTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `child-sealed content round-trips to the parent and never leaks in transit`() {
        val parent = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val plaintext = "don't tell your mom — it's our secret"

        // --- Child device: seal the content, embed the envelope in the synced record. ---
        val sealedEnvelope = EnvelopeCrypto.seal(plaintext, parent.public)
        val synced = ActivityRecord(
            id = "a1", childId = "c1", type = ActivityType.MESSAGE,
            createdAtEpochMillis = 1_700_000_000_000L,
            sourceApp = "com.instagram.android",
            content = json.encodeToString(SealedEnvelope.serializer(), sealedEnvelope),
        )

        // What the backend stores must not contain the plaintext anywhere.
        val onTheWire = json.encodeToString(ActivityRecord.serializer(), synced)
        assertNotEquals(true, onTheWire.contains("secret"), "plaintext must never appear in the synced payload")

        // --- Parent device: deserialize and open with the private key. ---
        val received = json.decodeFromString(ActivityRecord.serializer(), onTheWire)
        val envelope = json.decodeFromString(SealedEnvelope.serializer(), received.content)
        val opened = EnvelopeCrypto.openToString(envelope, parent.private)

        assertEquals(plaintext, opened)
    }
}
