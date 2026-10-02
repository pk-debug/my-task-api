package com.example.taskapi

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.example.taskapi.auth.ApiError
import com.example.taskapi.auth.AuthEmailSender
import com.example.taskapi.auth.AuthException
import com.example.taskapi.auth.AuthRepository
import com.example.taskapi.auth.AuthService
import com.example.taskapi.auth.ConsoleAuthEmailSender
import com.example.taskapi.auth.SmtpAuthEmailSender
import com.example.taskapi.auth.authRoutes
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = { module() })
        .start(wait = true)
}

fun Application.module() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val jwtSecret = environment.config.propertyOrNull("auth.jwtSecret")?.getString()
        ?.takeIf { it.isNotBlank() }
        ?: System.getenv("JWT_SECRET")
        ?: error("Set JWT_SECRET to a random value containing at least 32 bytes")
    val jdbcUrl = environment.config.propertyOrNull("auth.jdbcUrl")?.getString() ?: "jdbc:sqlite:auth.db"
    val mailHost = System.getenv("MAIL_HOST").orEmpty()
    val emailSender = if (mailHost.isBlank()) {
        ConsoleAuthEmailSender()
    } else {
        SmtpAuthEmailSender(
            host = mailHost,
            port = System.getenv("MAIL_PORT")?.toIntOrNull() ?: 587,
            username = System.getenv("MAIL_USERNAME").orEmpty(),
            password = System.getenv("MAIL_PASSWORD").orEmpty(),
            from = System.getenv("MAIL_FROM") ?: "no-reply@example.com",
            startTls = System.getenv("MAIL_SMTP_STARTTLS")?.toBooleanStrictOrNull() ?: true
        )
    }
    val publicBaseUrl = System.getenv("APP_PUBLIC_BASE_URL") ?: "http://localhost:$port"
    module(jwtSecret, jdbcUrl, emailSender, publicBaseUrl)
}

fun Application.module(jwtSecret: String, jdbcUrl: String) {
    module(jwtSecret, jdbcUrl, ConsoleAuthEmailSender(), "http://localhost:8081")
}

fun Application.module(jwtSecret: String, jdbcUrl: String, emailSender: AuthEmailSender, publicBaseUrl: String) {
    val authRepository = AuthRepository(jdbcUrl)
    val authService = AuthService(authRepository, jwtSecret, emailSender = emailSender, publicBaseUrl = publicBaseUrl)
    monitor.subscribe(ApplicationStopped) { authRepository.close() }

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request"))
        }
        exception<ContentTransformationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError(cause.message ?: "Invalid request body"))
        }
        exception<AuthException> { call, cause ->
            call.respond(HttpStatusCode.fromValue(cause.statusCode), ApiError(cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled request failure", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("Internal server error"))
        }
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Task API"
            verifier(authService.verifier)
            validate { credential ->
                val userId = credential.payload.subject?.toLongOrNull()
                if (userId != null && authRepository.findUserById(userId) != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ApiError("A valid bearer token is required"))
            }
        }
    }

    val taskService = TaskService(TaskRepository())
    routing {
        authRoutes(authService)
        authenticate("auth-jwt") {
            get("/auth/me") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()
                call.respond(authService.profile(userId))
            }
            taskRoutes(taskService)
        }
    }
}
