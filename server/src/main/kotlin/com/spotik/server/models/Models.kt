package com.spotik.server.models

import kotlinx.serialization.Serializable

/* ═══════  Requests  ═══════ */

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
    val age: Int = 18,
    val city: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class TelegramAuthRequest(
    val telegramId: Long,
    val firstName: String,
    val username: String? = null,
    val photoUrl: String? = null,
    val authDate: Long,
    val hash: String,
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val age: Int? = null,
    val bio: String? = null,
    val city: String? = null,
    val avatarUrl: String? = null,
)

/* ═══════  Responses  ═══════ */

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val userId: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
)

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val bio: String?,
    val avatarUrl: String?,
    val email: String?,
)

