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

class MultiChildTest {

    private fun consent(childId: String, name: String, parentId: String) =
        """{"childId":"$childId","childAge":11,"buyerName":"A Parent","buyerAge":42,
            "guardianshipProofRef":"d","signedCertificateRef":"c",
            "parentId":"$parentId","childDisplayName":"$name"}"""

    @Test
    fun `two children under one parent list together and resolve their account`() = testApplication {
        application { module() }

        client.post("/api/v1/consent") { contentType(ContentType.Application.Json); setBody(consent("c1", "Amara", "p1")) }
        client.post("/api/v1/consent") { contentType(ContentType.Application.Json); setBody(consent("c2", "Noah", "p1")) }
        // A third child under a different parent must not appear.
        client.post("/api/v1/consent") { contentType(ContentType.Application.Json); setBody(consent("c9", "Other", "p2")) }

        val res = client.get("/api/v1/parents/p1/children")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("Amara"))
        assertTrue(body.contains("Noah"))
        assertTrue(!body.contains("Other"))
    }
}
