package com.helpdesk.app.domain.model

enum class UserRole {
    USER,
    AGENT,
    ADMIN;

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.lowercase()) {
                "user" -> USER
                "agent" -> AGENT
                "admin" -> ADMIN
                else -> USER
            }
        }
    }

    fun toApiValue(): String = name.lowercase()
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)

data class AuthSession(
    val user: User,
    val accessToken: String,
    val refreshToken: String
)

data class LoginCredentials(
    val email: String,
    val password: String
)

data class RegisterCredentials(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole = UserRole.USER
)
