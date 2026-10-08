package com.example.taskapi

import com.example.taskapi.auth.AuthEmailSender
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TaskRoutesTest {
    private val json = Json

    @Test
    fun `task routes require a bearer token`() = testApplication {
        application { module(TEST_SECRET, "jdbc:sqlite::memory:", RecordingAuthEmailSender(), "http://localhost:8081") }

        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        val healthJson = json.parseToJsonElement(health.bodyAsText()).jsonObject
        assertEquals("UP", healthJson["status"]!!.jsonPrimitive.content)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/tasks").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/tasks/search").status)
        assertEquals(HttpStatusCode.Unauthorized, client.get("/auth/me").status)
    }

    @Test
    fun `verification and recovery protect account and task flows`() = testApplication {
        val emailSender = RecordingAuthEmailSender()
        application { module(TEST_SECRET, "jdbc:sqlite::memory:", emailSender, "http://localhost:8081") }

        register("first@example.com")
        val unverifiedLogin = login("first@example.com", "a-secure-test-password")
        assertEquals(HttpStatusCode.Unauthorized, unverifiedLogin.status)

        val firstVerificationToken = emailSender.verificationTokens.getValue("first@example.com")
        val resendVerification = client.post("/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"first@example.com"}""")
        }
        assertEquals(HttpStatusCode.OK, resendVerification.status)
        val currentVerificationToken = emailSender.verificationTokens.getValue("first@example.com")
        assertNotEquals(firstVerificationToken, currentVerificationToken)

        val first = verifyEmail(currentVerificationToken)
        val firstAccess = first["accessToken"]!!.jsonPrimitive.content
        val firstRefresh = first["refreshToken"]!!.jsonPrimitive.content
        assertEquals(HttpStatusCode.Unauthorized, verifyEmailResponse(firstVerificationToken).status)

        val profile = client.get("/auth/me") { bearerAuth(firstAccess) }
        assertEquals(HttpStatusCode.OK, profile.status)
        assertTrue(profile.bodyAsText().contains("first@example.com"))

        val createdTask = client.post("/tasks") {
            bearerAuth(firstAccess)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"private task","tags":["api"]}""")
        }
        assertEquals(HttpStatusCode.Created, createdTask.status)
        val taskId = json.parseToJsonElement(createdTask.bodyAsText())
            .jsonObject["id"]!!.jsonPrimitive.content

        val secondTask = client.post("/tasks") {
            bearerAuth(firstAccess)
            contentType(ContentType.Application.Json)
            setBody("""{"title":"planning checklist","status":"IN_PROGRESS"}""")
        }
        assertEquals(HttpStatusCode.Created, secondTask.status)

        val firstPage = client.get("/tasks/search?page=0&size=1") { bearerAuth(firstAccess) }
        assertEquals(HttpStatusCode.OK, firstPage.status)
        val firstPageJson = json.parseToJsonElement(firstPage.bodyAsText()).jsonObject
        assertEquals("2", firstPageJson["totalElements"]!!.jsonPrimitive.content)
        assertEquals("2", firstPageJson["totalPages"]!!.jsonPrimitive.content)
        assertEquals(1, firstPageJson["items"]!!.let { it as kotlinx.serialization.json.JsonArray }.size)

        val secondPage = client.get("/tasks/search?page=1&size=1") { bearerAuth(firstAccess) }
        assertTrue(secondPage.bodyAsText().contains("planning checklist"))

        val filteredPage = client.get("/tasks/search?q=private&status=TODO") { bearerAuth(firstAccess) }
        assertEquals(HttpStatusCode.OK, filteredPage.status)
        assertTrue(filteredPage.bodyAsText().contains("private task"))
        val filteredPageJson = json.parseToJsonElement(filteredPage.bodyAsText()).jsonObject
        assertEquals("1", filteredPageJson["totalElements"]!!.jsonPrimitive.content)

        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/tasks/search?size=101") { bearerAuth(firstAccess) }.status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/tasks/search?page=not-a-number") { bearerAuth(firstAccess) }.status
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            client.get("/tasks/search?status=UNKNOWN") { bearerAuth(firstAccess) }.status
        )

        register("second@example.com")
        val second = verifyEmail(emailSender.verificationTokens.getValue("second@example.com"))
        val secondAccess = second["accessToken"]!!.jsonPrimitive.content
        val secondTasks = client.get("/tasks") { bearerAuth(secondAccess) }
        assertEquals("[]", secondTasks.bodyAsText().trim())
        val secondSearch = client.get("/tasks/search") { bearerAuth(secondAccess) }
        val secondSearchJson = json.parseToJsonElement(secondSearch.bodyAsText()).jsonObject
        assertEquals("0", secondSearchJson["totalElements"]!!.jsonPrimitive.content)
        val otherUsersTask = client.get("/tasks/$taskId") { bearerAuth(secondAccess) }
        assertEquals(HttpStatusCode.NotFound, otherUsersTask.status)

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

        val activeSession = login("first@example.com", "a-secure-test-password")
        assertEquals(HttpStatusCode.OK, activeSession.status)
        val activeRefresh = json.parseToJsonElement(activeSession.bodyAsText())
            .jsonObject["refreshToken"]!!.jsonPrimitive.content

        val forgotResponse = client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"first@example.com"}""")
        }
        assertEquals(HttpStatusCode.OK, forgotResponse.status)
        val unknownForgotResponse = client.post("/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"missing@example.com"}""")
        }
        assertEquals(forgotResponse.bodyAsText(), unknownForgotResponse.bodyAsText())

        val resetToken = emailSender.resetTokens.getValue("first@example.com")
        val reset = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"$resetToken","newPassword":"a-new-secure-password"}""")
        }
        assertEquals(HttpStatusCode.OK, reset.status)

        val resetRevokedRefresh = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$activeRefresh"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resetRevokedRefresh.status)
        assertEquals(HttpStatusCode.Unauthorized, login("first@example.com", "a-secure-test-password").status)
        assertEquals(HttpStatusCode.OK, login("first@example.com", "a-new-secure-password").status)

        val reusedReset = client.post("/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"token":"$resetToken","newPassword":"another-secure-password"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, reusedReset.status)
    }

    private suspend fun ApplicationTestBuilder.register(email: String) {
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"a-secure-test-password","name":"Test User"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
    }

    private suspend fun ApplicationTestBuilder.verifyEmail(token: String): JsonObject {
        val response = verifyEmailResponse(token)
        assertEquals(HttpStatusCode.OK, response.status)
        return json.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.verifyEmailResponse(token: String) = client.post("/auth/verify-email") {
        contentType(ContentType.Application.Json)
        setBody("""{"token":"$token"}""")
    }

    private suspend fun ApplicationTestBuilder.login(email: String, password: String) = client.post("/auth/login") {
        contentType(ContentType.Application.Json)
        setBody("""{"email":"$email","password":"$password"}""")
    }

    private class RecordingAuthEmailSender : AuthEmailSender {
        val verificationTokens = mutableMapOf<String, String>()
        val resetTokens = mutableMapOf<String, String>()

        override fun sendVerification(email: String, token: String, endpoint: String) {
            verificationTokens[email] = token
        }

        override fun sendPasswordReset(email: String, token: String, endpoint: String) {
            resetTokens[email] = token
        }
    }

    private companion object {
        const val TEST_SECRET = "test-only-secret-which-is-long-enough-for-hmac"
    }
}
