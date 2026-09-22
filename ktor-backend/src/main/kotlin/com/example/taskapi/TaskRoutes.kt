package com.example.taskapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.taskRoutes(repository: TaskRepository) {
    route("/tasks") {
        get {
            call.respond(repository.getAll())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

            val task = repository.getById(id)
            if (task == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            } else {
                call.respond(task)
            }
        }

        post {
            val request = call.receive<TaskRequest>()
            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Title is required"))
                return@post
            }

            val createdTask = repository.create(request)
            call.respond(HttpStatusCode.Created, createdTask)
        }

        put("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

            val request = call.receive<TaskRequest>()
            if (request.title.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Title is required"))
                return@put
            }

            val updatedTask = repository.update(id, request)
            if (updatedTask == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            } else {
                call.respond(updatedTask)
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid id"))

            val deleted = repository.delete(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Task not found"))
            }
        }
    }
}
