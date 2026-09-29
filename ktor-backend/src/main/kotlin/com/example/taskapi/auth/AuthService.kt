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

class AuthException(val statusCode: Int, override val message: String) : RuntimeException(message)

class AuthService(
    private val repository: AuthRepository,
    secret: String,
    private val issuer: String = "task-api"
) {
    private val accessTtlSeconds = 15 * 60L
    private val refreshTtlMillis = 7 * 24 * 60 * 60 * 1000L
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).withIssuer(issuer).build()

    init {
        require(secret.toByteArray(Charsets.UTF_8).size >= 32) { "JWT_SECRET must contain at least 32 bytes" }
    }

    fun register(request: RegisterRequest): TokenResponse {
        val email = normalizeEmail(request.email)
        validateCredentials(email, request.password)
        if (request.name.isBlank() || request.name.trim().length > 80) {
            throw AuthException(400, "Name is required and must be at most 80 characters")
        }
        if (repository.findUserByEmail(email) != null) {
            throw AuthException(409, "An account with this email already exists")
        }
        val user = repository.createUser(email, request.name.trim(), BCrypt.hashpw(request.password, BCrypt.gensalt(12)))
        return issueTokens(user)
    }

    fun login(request: LoginRequest): TokenResponse {
        val email = normalizeEmail(request.email)
        val user = repository.findUserByEmail(email)
        if (user == null || request.password.isEmpty() || !BCrypt.checkpw(request.password, user.passwordHash)) {
            throw AuthException(401, "Invalid email or password")
        }
        return issueTokens(user)
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

    private fun validateCredentials(email: String, password: String) {
        if (!email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) || email.length > 254) {
            throw AuthException(400, "A valid email address is required")
        }
        if (password.length < 8 || password.toByteArray(Charsets.UTF_8).size > 72) {
            throw AuthException(400, "Password must be at least 8 characters and at most 72 bytes")
        }
    }

    private fun normalizeEmail(email: String) = email.trim().lowercase()

    private fun hash(value: String) = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun AuthUser.toResponse() = UserResponse(id, email, name)
}