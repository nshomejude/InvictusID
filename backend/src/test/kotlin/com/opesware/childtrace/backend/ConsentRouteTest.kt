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

class ConsentRouteTest {

    @Test
    fun `health responds ok`() = testApplication {
        application { module() }
        val res = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.OK, res.status)
    }

    @Test
    fun `consent for a supported age is accepted and maps to a band`() = testApplication {
        application { module() }
        val res = client.post("/api/v1/consent") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"childId":"c1","childAge":11,"buyerName":"A Parent","buyerAge":42,
                   "guardianshipProofRef":"doc-1","signedCertificateRef":"cert-1","parentId":"p1"}"""
            )
        }
        assertEquals(HttpStatusCode.Created, res.status)
        assertTrue(res.bodyAsText().contains("FLAG_HIGH"))
    }

    @Test
    fun `consent at or above the ceiling is refused`() = testApplication {
        application { module() }
        val res = client.post("/api/v1/consent") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"childId":"c2","childAge":16,"buyerName":"A Parent","buyerAge":42,
                   "guardianshipProofRef":"doc-2","signedCertificateRef":"cert-2","parentId":"p1"}"""
            )
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
        assertTrue(res.bodyAsText().contains("provisioning_refused"))
    }

    @Test
    fun `pairing without a consent record is refused`() = testApplication {
        application { module() }
        val res = client.post("/api/v1/devices/pair") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"nope","pairingToken":"tok","appVersion":"0.1.0"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }
}
