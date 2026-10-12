package com.opesware.childtrace.backend

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OnboardingFlowTest {

    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun codeFrom(body: String): String =
        json.parseToJsonElement(body).jsonObject["code"]!!.jsonPrimitive.content

    @Test
    fun `full onboarding sequence provisions a child end to end`() = testApplication {
        application { module() }

        // 1. Agent records consent for an 11-year-old.
        val consent = client.post("/api/v1/consent") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","childAge":11,"buyerName":"Parent","buyerAge":42,
                       "guardianshipProofRef":"d","signedCertificateRef":"c","parentId":"p1"}""")
        }
        assertEquals(HttpStatusCode.Created, consent.status)

        // 2. Agent opens a pairing session.
        val session = client.post("/api/v1/pairing-sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1"}""")
        }
        assertEquals(HttpStatusCode.Created, session.status)
        val code = codeFrom(session.bodyAsText())

        // 3. Child cannot pair before the parent claims.
        val early = client.post("/api/v1/devices/pair") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","pairingToken":"$code","appVersion":"0.1.0"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, early.status)

        // 4. Parent claims the session and registers its key.
        val claim = client.post("/api/v1/pairing-sessions/claim") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"$code","keyId":"kid-1","publicKeySpki":"SPKI=="}""")
        }
        assertEquals(HttpStatusCode.OK, claim.status)
        assertTrue(claim.bodyAsText().contains("FLAG_HIGH"))

        // 5. Child pairs successfully now.
        val paired = client.post("/api/v1/devices/pair") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","pairingToken":"$code","appVersion":"0.1.0"}""")
        }
        assertEquals(HttpStatusCode.Created, paired.status)
    }

    @Test
    fun `a session cannot be opened without consent`() = testApplication {
        application { module() }
        val res = client.post("/api/v1/pairing-sessions") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"no-consent"}""")
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }
}
