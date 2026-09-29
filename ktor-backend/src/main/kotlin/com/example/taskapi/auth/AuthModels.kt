package com.example.taskapi.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class AuthUser(
    val id: Long,
    val email: String,
    val name: String,
    val passwordHash: String
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val name: String
)

@Serializable
data class TokenResponse(
    val tokenType: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class ApiError(val error: String)