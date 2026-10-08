package com.example.taskapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import com.example.taskapi.auth.ApiError

fun Route.taskRoutes(service: TaskService) {
    route("/tasks") {
        get("/search") {
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()
            val query = call.request.queryParameters["q"]
            val rawStatus = call.request.queryParameters["status"]
            val status = rawStatus?.takeIf { it.isNotBlank() }?.let { value ->
                TaskStatus.entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("Invalid task status"))
            }
            val pageParameter = call.request.queryParameters["page"]
            val page = if (pageParameter == null) 0 else pageParameter.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("page must be an integer"))
            val sizeParameter = call.request.queryParameters["size"]
            val size = if (sizeParameter == null) 20 else sizeParameter.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ApiError("size must be an integer"))
            try {
                call.respond(service.search(ownerId, query, status, page, size))
            } catch (exception: InvalidTaskSearchException) {
                call.respond(HttpStatusCode.BadRequest, ApiError(exception.message ?: "Invalid pagination"))
            }
        }

        get {
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()
            call.respond(service.getAll(ownerId))
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()

            val task = service.getById(id, ownerId)
            if (task == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            } else {
                call.respond(task)
            }
        }

        post {
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()
            val request = call.receive<TaskRequest>()
            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Title is required"))
                return@post
            }

            val createdTask = service.create(request, ownerId)
            call.respond(HttpStatusCode.Created, createdTask)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()

            val request = call.receive<TaskRequest>()
            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Title is required"))
                return@put
            }

            val updatedTask = service.update(id, request, ownerId)
            if (updatedTask == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            } else {
                call.respond(updatedTask)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))
            val ownerId = call.principal<JWTPrincipal>()!!.payload.subject.toLong()

            val deleted = service.delete(id, ownerId)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            }
        }
    }
}
