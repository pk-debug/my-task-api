package com.example.taskapi.auth

import java.sql.Connection
import java.sql.DriverManager

class AuthRepository(jdbcUrl: String) : AutoCloseable {
    private val connection: Connection = DriverManager.getConnection(jdbcUrl)

    init {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    email TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    password_hash TEXT NOT NULL
                )""".trimIndent()
            )
            statement.executeUpdate(
                """CREATE TABLE IF NOT EXISTS refresh_tokens (
                    token_hash TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    revoked INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )""".trimIndent()
            )
        }
    }

    @Synchronized
    fun findUserByEmail(email: String): AuthUser? = connection.prepareStatement(
        "SELECT id, email, name, password_hash FROM users WHERE email = ?"
    ).use { statement ->
        statement.setString(1, email)
        statement.executeQuery().use { result -> if (result.next()) result.toUser() else null }
    }

    @Synchronized
    fun findUserById(id: Long): AuthUser? = connection.prepareStatement(
        "SELECT id, email, name, password_hash FROM users WHERE id = ?"
    ).use { statement ->
        statement.setLong(1, id)
        statement.executeQuery().use { result -> if (result.next()) result.toUser() else null }
    }

    @Synchronized
    fun createUser(email: String, name: String, passwordHash: String): AuthUser {
        try {
            connection.prepareStatement(
                "INSERT INTO users(email, name, password_hash) VALUES (?, ?, ?)"
            ).use { statement ->
                statement.setString(1, email)
                statement.setString(2, name)
                statement.setString(3, passwordHash)
                statement.executeUpdate()
            }
        } catch (exception: java.sql.SQLException) {
            if (exception.message.orEmpty().contains("UNIQUE", ignoreCase = true)) {
                throw AuthException(409, "An account with this email already exists")
            }
            throw exception
        }
        val id = connection.createStatement().use { statement ->
            statement.executeQuery("SELECT last_insert_rowid()").use { result -> result.next(); result.getLong(1) }
        }
        return AuthUser(id, email, name, passwordHash)
    }

    @Synchronized
    fun storeRefreshToken(tokenHash: String, userId: Long, expiresAtMillis: Long) {
        connection.prepareStatement(
            "INSERT INTO refresh_tokens(token_hash, user_id, expires_at) VALUES (?, ?, ?)"
        ).use { statement ->
            statement.setString(1, tokenHash)
            statement.setLong(2, userId)
            statement.setLong(3, expiresAtMillis)
            statement.executeUpdate()
        }
    }

    @Synchronized
    fun consumeRefreshToken(tokenHash: String, nowMillis: Long): AuthUser? {
        connection.autoCommit = false
        try {
            val userId = connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked = 1 WHERE token_hash = ? AND revoked = 0 AND expires_at > ? RETURNING user_id"
            ).use { statement ->
                statement.setString(1, tokenHash)
                statement.setLong(2, nowMillis)
                statement.executeQuery().use { result -> if (result.next()) result.getLong(1) else null }
            }
            val user = userId?.let { findUserById(it) }
            if (user == null) {
                connection.rollback()
                return null
            }
            connection.commit()
            return user
        } finally {
            connection.autoCommit = true
        }
    }

    @Synchronized
    fun revokeRefreshToken(tokenHash: String) {
        connection.prepareStatement("UPDATE refresh_tokens SET revoked = 1 WHERE token_hash = ?").use { statement ->
            statement.setString(1, tokenHash)
            statement.executeUpdate()
        }
    }

    override fun close() = connection.close()

    private fun java.sql.ResultSet.toUser() = AuthUser(
        id = getLong("id"),
        email = getString("email"),
        name = getString("name"),
        passwordHash = getString("password_hash")
    )
}