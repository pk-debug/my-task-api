package com.example.taskapi.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.Locale

class AuthException(val statusCode: Int, override val message: String) : RuntimeException(message)

class AuthService(
    private val repository: AuthRepository,
    secret: String,
    private val issuer: String = "task-api",
    private val emailSender: AuthEmailSender = ConsoleAuthEmailSender(),
    private val publicBaseUrl: String = "http://localhost:8081"
) {
    private val accessTtlSeconds = 15 * 60L
    private val refreshTtlMillis = 7 * 24 * 60 * 60 * 1000L
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build()

    init {
        require(secret.toByteArray(Charsets.UTF_8).size >= 32) { "JWT_SECRET must contain at least 32 bytes" }
    }

    fun register(request: RegisterRequest): MessageResponse {
        val email = normalizeEmail(request.email)
        validateCredentials(email, request.password)
        if (request.name.isBlank() || request.name.trim().length > 80) {
            throw AuthException(400, "Name is required and must be at most 80 characters")
        }
        if (repository.findUserByEmail(email) != null) {
            throw AuthException(409, "An account with this email already exists")
        }
        val user = repository.createUser(email, request.name.trim(), BCrypt.hashpw(request.password, BCrypt.gensalt(12)))
        sendActionToken(user, "EMAIL_VERIFICATION", 24 * 60 * 60 * 1000L) { token ->
            emailSender.sendVerification(user.email, token, "$publicBaseUrl/auth/verify-email")
        }
        return MessageResponse("Check your email for a verification token")
    }

    fun login(request: LoginRequest): TokenResponse {
        val email = normalizeEmail(request.email)
        val user = repository.findUserByEmail(email)
        if (user == null || !user.emailVerified || request.password.isEmpty() || !BCrypt.checkpw(request.password, user.passwordHash)) {
            throw AuthException(401, "Invalid email or password")
        }
        return issueTokens(user)
    }

    fun verifyEmail(rawToken: String): TokenResponse {
        if (rawToken.isBlank()) throw AuthException(401, "Invalid verification token")
        val user = repository.verifyEmailWithToken(hash(rawToken), System.currentTimeMillis())
            ?: throw AuthException(401, "Invalid or expired verification token")
        return issueTokens(user)
    }

    fun resendVerification(emailAddress: String): MessageResponse {
        repository.findUserByEmail(normalizeEmail(emailAddress))
            ?.takeIf { !it.emailVerified }
            ?.let { user ->
                sendActionToken(user, "EMAIL_VERIFICATION", 24 * 60 * 60 * 1000L) { token ->
                    emailSender.sendVerification(user.email, token, "$publicBaseUrl/auth/verify-email")
                }
            }
        return MessageResponse("If the account needs verification, a verification message has been sent")
    }

    fun forgotPassword(emailAddress: String): MessageResponse {
        repository.findUserByEmail(normalizeEmail(emailAddress))
            ?.takeIf { it.emailVerified }
            ?.let { user ->
                sendActionToken(user, "PASSWORD_RESET", 60 * 60 * 1000L) { token ->
                    emailSender.sendPasswordReset(user.email, token, "$publicBaseUrl/auth/reset-password")
                }
            }
        return MessageResponse("If the account is verified, a password reset message has been sent")
    }

    fun resetPassword(request: ResetPasswordRequest): MessageResponse {
        validateNewPassword(request.newPassword)
        if (!repository.resetPasswordWithToken(hash(request.token), System.currentTimeMillis(), BCrypt.hashpw(request.newPassword, BCrypt.gensalt(12)))) {
            throw AuthException(401, "Invalid or expired password reset token")
        }
        return MessageResponse("Password updated; log in again with the new password")
    }

    fun refresh(rawToken: String): TokenResponse {
        if (rawToken.isBlank()) throw AuthException(401, "Invalid refresh token")
        val user = repository.consumeRefreshToken(hash(rawToken), System.currentTimeMillis())
            ?: throw AuthException(401, "Invalid refresh token")
        return issueTokens(user)
    }

    fun logout(rawToken: String) {
        if (rawToken.isNotBlank()) repository.revokeRefreshToken(hash(rawToken))
    }

    fun profile(userId: Long): UserResponse {
        val user = repository.findUserById(userId) ?: throw AuthException(401, "Account no longer exists")
        return user.toResponse()
    }

    private fun issueTokens(user: AuthUser): TokenResponse {
        val now = Instant.now()
        val accessToken = JWT.create()
            .withIssuer(issuer)
            .withSubject(user.id.toString())
            .withClaim("email", user.email)
            .withClaim("name", user.name)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(accessTtlSeconds)))
            .sign(algorithm)
        val randomBytes = ByteArray(32).also(SecureRandom()::nextBytes)
        val refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
        repository.storeRefreshToken(hash(refreshToken), user.id, now.toEpochMilli() + refreshTtlMillis)
        return TokenResponse("Bearer", accessToken, refreshToken, accessTtlSeconds, user.toResponse())
    }

    private fun sendActionToken(user: AuthUser, purpose: String, ttlMillis: Long, deliver: (String) -> Unit) {
        val rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))
        repository.createActionToken(hash(rawToken), user.id, purpose, System.currentTimeMillis() + ttlMillis)
        deliver(rawToken)
    }

    private fun validateCredentials(email: String, password: String) {
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) || email.length > 254) {
            throw AuthException(400, "A valid email address is required")
        }
        validateNewPassword(password)
    }

    private fun validateNewPassword(password: String) {
        if (password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            throw AuthException(400, "Password must be at least 8 characters and at most 72 bytes")
        }
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase(Locale.ROOT)

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun AuthUser.toResponse() = UserResponse(id, email, name)
}