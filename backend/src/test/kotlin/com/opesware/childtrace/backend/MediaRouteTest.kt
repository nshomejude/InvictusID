package com.opesware.childtrace.backend

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaRouteTest {

    private fun alertBatch(childId: String, alertId: String, severity: String) =
        """{"childId":"$childId","alerts":[{"id":"$alertId","childId":"$childId",
            "category":"GROOMING","severity":"$severity","createdAtEpochMillis":1000,
            "sourceApp":"com.whatsapp","contextSnippet":"enc","reason":"test","status":"NEW"}]}"""

    private fun upload(childId: String, mediaId: String, trigger: String, alertId: String?, kind: String = "IMAGE") =
        """{"media":{"id":"$mediaId","childId":"$childId","kind":"$kind","trigger":"$trigger",
            ${alertId?.let { "\"alertId\":\"$it\"," } ?: ""}
            "mimeType":"image/png","capturedAtEpochMillis":2000},
            "envelope":{"keyId":"kid-1","wrappedKey":"V1JBUA==","iv":"SVY=","ciphertext":"Q0lQSEVS"}}"""

    @Test
    fun `flagged media for a high alert is accepted, listed, and fetched sealed`() = testApplication {
        application { module() }

        client.post("/api/v1/alerts") {
            contentType(ContentType.Application.Json); setBody(alertBatch("c1", "a1", "HIGH"))
        }

        val up = client.post("/api/v1/media") {
            contentType(ContentType.Application.Json); setBody(upload("c1", "m1", "FLAGGED_ALERT", "a1"))
        }
        assertEquals(HttpStatusCode.Accepted, up.status)

        val list = client.get("/api/v1/children/c1/media")
        assertEquals(HttpStatusCode.OK, list.status)
        assertTrue(list.bodyAsText().contains("m1"))
        // The list carries metadata + keyId, never the ciphertext.
        assertTrue(!list.bodyAsText().contains("Q0lQSEVS"))

        val one = client.get("/api/v1/children/c1/media/m1")
        assertEquals(HttpStatusCode.OK, one.status)
        // The single-item fetch returns the sealed blob for the parent to decrypt.
        assertTrue(one.bodyAsText().contains("Q0lQSEVS"))
    }

    @Test
    fun `flagged media with no linked alert is refused - the no-bulk guard`() = testApplication {
        application { module() }
        val up = client.post("/api/v1/media") {
            contentType(ContentType.Application.Json); setBody(upload("c1", "m2", "FLAGGED_ALERT", null))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, up.status)
    }

    @Test
    fun `flagged media for a below-floor alert is refused`() = testApplication {
        application { module() }
        client.post("/api/v1/alerts") {
            contentType(ContentType.Application.Json); setBody(alertBatch("c1", "low1", "LOW"))
        }
        val up = client.post("/api/v1/media") {
            contentType(ContentType.Application.Json); setBody(upload("c1", "m3", "FLAGGED_ALERT", "low1"))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, up.status)
    }

    @Test
    fun `explicit screen capture is refused by default (counsel gate off)`() = testApplication {
        application { module() }
        val up = client.post("/api/v1/media") {
            contentType(ContentType.Application.Json)
            setBody(upload("c1", "m4", "PARENT_REQUESTED_SCREEN", null, kind = "SCREENSHOT"))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, up.status)
    }
}
