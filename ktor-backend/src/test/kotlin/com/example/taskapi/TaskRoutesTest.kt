package com.example.taskapi

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRoutesTest {
    @Test
    fun `should return empty list initially`() = testApplication {
        application {
            module()
        }

        val response = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `should create a task`() = testApplication {
        application {
            module()
        }

        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Prepare Kotlin answer","done":false}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Prepare Kotlin answer"))
    }
}
