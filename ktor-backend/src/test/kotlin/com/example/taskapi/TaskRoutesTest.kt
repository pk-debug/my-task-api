package com.example.taskapi

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskRoutesTest {
    private val json = Json

    @Test
    fun `task routes require a bearer token`() = testApplication {
        application { module(TEST_SECRET, "jdbc:sqlite::memory:") }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/tasks").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/auth/me").status)
    }

    @Test
    fun `register login refresh logout and owner scoped tasks work`() = testApplication {
        application { module(TEST_SECRET, "jdbc:sqlite::memory:") }

        val first = register("first@example.com")
        val firstAccess = first["accessToken"]!!.jsonPrimitive.content
        val firstRefresh = first["refreshToken"]!!.jsonPrimitive.content

        val profile = client.get("/auth/me") { bearerAuth(firstAccess) }
        assertEquals(HttpStatusCode.OK, profile.status)
        assertTrue(profile.bodyAsText().contains("first@example.com"))

        val createdTask = client.post("/tasks") {
            bearerAuth(firstAccess)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"private task","tags":["api"]}""")
        }
        assertEquals(HttpStatusCode.Created, createdTask.status)

        val second = register("second@example.com")
        val secondAccess = second["accessToken"]!!.jsonPrimitive.content
        val secondTasks = client.get("/tasks") { bearerAuth(secondAccess) }
        assertEquals("[]", secondTasks.bodyAsText().trim())

        val refreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$firstRefresh"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val rotatedRefresh = json.parseToJsonElement(refreshResponse.bodyAsText())
            .jsonObject["refreshToken"]!!.jsonPrimitive.content

        val reusedRefresh = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$firstRefresh"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, reusedRefresh.status)

        val logout = client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$rotatedRefresh"}""")
        }
        assertEquals(HttpStatusCode.NoContent, logout.status)

        val revokedRefresh = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$rotatedRefresh"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, revokedRefresh.status)

        val login = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"first@example.com","password":"a-secure-test-password"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)
    }

    private suspend fun ApplicationTestBuilder.register(email: String) = client.post("/auth/register") {
        contentType(ContentType.Application.Json)
        setBody("""{"email":"$email","password":"a-secure-test-password","name":"Test User"}""")
    }.let { response ->
        assertEquals(HttpStatusCode.Created, response.status)
        json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private companion object {
        const val TEST_SECRET = "test-only-secret-which-is-long-enough-for-hmac"
    }
}
