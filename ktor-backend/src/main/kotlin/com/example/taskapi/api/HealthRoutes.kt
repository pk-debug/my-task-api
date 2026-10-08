package com.example.taskapi.api

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val application: String,
    val timestamp: String,
    val uptimeSeconds: Long
)

fun Route.healthRoutes(startedAt: Instant) {
    get("/health") {
        call.respond(
            HealthResponse(
                status = "UP",
                application = "task-api",
                timestamp = Instant.now().toString(),
                uptimeSeconds = Duration.between(startedAt, Instant.now()).seconds.coerceAtLeast(0)
            )
        )
    }
}