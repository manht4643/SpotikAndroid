package com.spotik.server.routes

import com.spotik.server.auth.JwtConfig
import com.spotik.server.database.Users
import com.spotik.server.models.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

fun Route.authRoutes() {

    /* ═══════  POST /api/register  ═══════ */
    post("/register") {
        val req = call.receive<RegisterRequest>()

        // Validate
        if (req.email.isBlank() || !req.email.contains("@")) {
            call.respond(HttpStatusCode.BadRequest,
                AuthResponse(false, message = "Некорректный email"))
            return@post
        }
        if (req.password.length < 6) {
            call.respond(HttpStatusCode.BadRequest,
                AuthResponse(false, message = "Пароль должен быть не менее 6 символов"))
            return@post
        }

        // Check if email already taken
        val exists = transaction {
            Users.selectAll().where { Users.email eq req.email.lowercase().trim() }.count() > 0
        }
        if (exists) {
            call.respond(HttpStatusCode.Conflict,
                AuthResponse(false, message = "Аккаунт с этим email уже существует"))
            return@post
        }

        // Create user
        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt(12))
        val userId = transaction {
            Users.insert {
                it[email] = req.email.lowercase().trim()
                it[passwordHash] = hash
                it[name] = req.name.ifBlank { "Пользователь" }
                it[age] = req.age.coerceIn(14, 99)
                it[city] = req.city
                it[bio] = req.bio ?: ""
                it[avatarUrl] = req.avatarUrl
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }[Users.id].toString()
        }

        val token = JwtConfig.generateToken(userId, req.email.lowercase().trim())

        call.respond(HttpStatusCode.Created,
            AuthResponse(success = true, token = token, userId = userId))
    }

    /* ═══════  POST /api/login  ═══════ */
    post("/login") {
        val req = call.receive<LoginRequest>()

        val user = transaction {
            Users.selectAll()
                .where { Users.email eq req.email.lowercase().trim() }
                .firstOrNull()
        }

        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized,
                AuthResponse(false, message = "Аккаунт не найден"))
            return@post
        }

        if (!BCrypt.checkpw(req.password, user[Users.passwordHash])) {
            call.respond(HttpStatusCode.Unauthorized,
                AuthResponse(false, message = "Неверный пароль"))
            return@post
        }

        val userId = user[Users.id].toString()
        val token = JwtConfig.generateToken(userId, user[Users.email])

        call.respond(AuthResponse(success = true, token = token, userId = userId))
    }

    /* ═══════  POST /api/logout  ═══════ */
    post("/logout") {
        // Stateless JWT — client simply drops the token.
        // If you need server-side revocation, add a blacklist table.
        call.respond(ApiResponse(success = true, message = "Logged out"))
    }

    /* ═══════  POST /api/auth/telegram  ═══════ */
    post("/auth/telegram") {
        val req = call.receive<TelegramAuthRequest>()

        // TODO: Verify hash with Telegram Bot token (HMAC-SHA256)
        // For now, trust the payload (implement verification before production)

        // Find or create user by telegram_id
        val existingUser = transaction {
            Users.selectAll()
                .where { Users.telegramId eq req.telegramId }
                .firstOrNull()
        }

        if (existingUser != null) {
            val userId = existingUser[Users.id].toString()
            val token = JwtConfig.generateToken(userId, existingUser[Users.email])
            call.respond(AuthResponse(success = true, token = token, userId = userId))
        } else {
            // Create new user from Telegram data
            val email = "tg_${req.telegramId}@spotik.local"
            val hash = BCrypt.hashpw(java.util.UUID.randomUUID().toString(), BCrypt.gensalt(12))

            val userId = transaction {
                Users.insert {
                    it[Users.email] = email
                    it[passwordHash] = hash
                    it[name] = req.firstName
                    it[telegramId] = req.telegramId
                    it[avatarUrl] = req.photoUrl
                    it[createdAt] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }[Users.id].toString()
            }

            val token = JwtConfig.generateToken(userId, email)
            call.respond(HttpStatusCode.Created,
                AuthResponse(success = true, token = token, userId = userId))
        }
    }
}

