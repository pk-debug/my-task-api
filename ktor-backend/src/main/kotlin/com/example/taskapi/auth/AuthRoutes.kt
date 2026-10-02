package com.example.taskapi.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            call.respond(HttpStatusCode.Created, authService.register(call.receive<RegisterRequest>()))
        }

        post("/login") {
            call.respond(authService.login(call.receive<LoginRequest>()))
        }

        post("/verify-email") {
            call.respond(authService.verifyEmail(call.receive<VerifyEmailRequest>().token))
        }

        post("/resend-verification") {
            call.respond(authService.resendVerification(call.receive<EmailRequest>().email))
        }

        post("/forgot-password") {
            call.respond(authService.forgotPassword(call.receive<EmailRequest>().email))
        }

        post("/reset-password") {
            call.respond(authService.resetPassword(call.receive<ResetPasswordRequest>()))
        }

        post("/refresh") {
            call.respond(authService.refresh(call.receive<RefreshRequest>().refreshToken))
        }

        post("/logout") {
            authService.logout(call.receive<RefreshRequest>().refreshToken)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}