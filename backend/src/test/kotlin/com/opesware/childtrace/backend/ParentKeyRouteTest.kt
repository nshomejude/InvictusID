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

class ParentKeyRouteTest {

    @Test
    fun `parent key can be registered then fetched by the child`() = testApplication {
        application { module() }

        val register = client.post("/api/v1/parent-key") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","keyId":"kid-1","publicKeySpki":"BASE64SPKI=="}""")
        }
        assertEquals(HttpStatusCode.Created, register.status)

        val fetch = client.get("/api/v1/children/c1/parent-key")
        assertEquals(HttpStatusCode.OK, fetch.status)
        assertTrue(fetch.bodyAsText().contains("BASE64SPKI=="))
        assertTrue(fetch.bodyAsText().contains("kid-1"))
    }

    @Test
    fun `registering a new key rotates current to the newest`() = testApplication {
        application { module() }
        client.post("/api/v1/parent-key") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","keyId":"kid-1","publicKeySpki":"OLD=="}""")
        }
        client.post("/api/v1/parent-key") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","keyId":"kid-2","publicKeySpki":"NEW=="}""")
        }
        val fetch = client.get("/api/v1/children/c1/parent-key")
        assertTrue(fetch.bodyAsText().contains("kid-2"))
        assertTrue(fetch.bodyAsText().contains("NEW=="))
    }

    @Test
    fun `fetching a parent key that was never registered is refused`() = testApplication {
        application { module() }
        val fetch = client.get("/api/v1/children/unknown/parent-key")
        assertEquals(HttpStatusCode.UnprocessableEntity, fetch.status)
    }
}
