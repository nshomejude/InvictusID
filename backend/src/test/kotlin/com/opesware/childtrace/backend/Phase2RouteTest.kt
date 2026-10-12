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

class Phase2RouteTest {

    @Test
    fun `geofence crossing is reported when the child leaves a fence`() = testApplication {
        application { module() }

        // A fence at the school with a 150m radius.
        client.post("/api/v1/children/c1/geofences") {
            contentType(ContentType.Application.Json)
            setBody("""{"id":"g1","childId":"c1","label":"School","latitude":4.0611,"longitude":9.7679,"radiusMeters":150.0,"trigger":"BOTH"}""")
        }
        // First fix: inside.
        client.post("/api/v1/location") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","latitude":4.0611,"longitude":9.7679,"atEpochMillis":1000}""")
        }
        // Second fix: far away → EXITED.
        val res = client.post("/api/v1/location") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","latitude":4.0700,"longitude":9.7800,"atEpochMillis":2000}""")
        }
        assertEquals(HttpStatusCode.Accepted, res.status)
        assertTrue(res.bodyAsText().contains("EXITED"))
        assertTrue(res.bodyAsText().contains("School"))
    }

    @Test
    fun `push token registers and tamper events round-trip`() = testApplication {
        application { module() }
        val reg = client.post("/api/v1/push-token") {
            contentType(ContentType.Application.Json)
            setBody("""{"parentId":"p1","childId":"c1","token":"fcm-abc"}""")
        }
        assertEquals(HttpStatusCode.Created, reg.status)

        client.post("/api/v1/tamper") {
            contentType(ContentType.Application.Json)
            setBody("""{"childId":"c1","kind":"ACCESSIBILITY_DISABLED","atEpochMillis":1234}""")
        }
        val events = client.get("/api/v1/children/c1/tamper")
        assertTrue(events.bodyAsText().contains("ACCESSIBILITY_DISABLED"))
    }

    @Test
    fun `digest responds for a child with no alerts`() = testApplication {
        application { module() }
        val res = client.get("/api/v1/children/c1/digest")
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("quiet week"))
    }
}
