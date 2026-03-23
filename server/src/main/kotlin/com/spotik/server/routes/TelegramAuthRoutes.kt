package com.spotik.server.routes

import com.spotik.server.auth.JwtConfig
import com.spotik.server.database.TelegramAuthSessions
import com.spotik.server.database.Users
import com.spotik.server.models.AuthResponse
import com.spotik.server.telegram.TelegramBotService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.UUID

/* ═══════  Request / Response models  ═══════ */

@Serializable
data class TelegramInitResponse(
    val success: Boolean,
    val botUsername: String? = null,
    val nonce: String? = null,
    val deepLink: String? = null,
    val message: String? = null,
)

@Serializable
data class TelegramPollResponse(
    val success: Boolean,
    val confirmed: Boolean = false,
    val token: String? = null,
    val userId: String? = null,
    val message: String? = null,
)

/* ═══════  Routes  ═══════ */

fun Route.telegramAuthRoutes() {

    /* ── POST /api/auth/telegram/init ──
     * Client calls this to start Telegram login.
     * Returns a nonce and a deep link to the bot.
     */
    post("/auth/telegram/init") {
        if (!TelegramBotService.enabled) {
            call.respond(HttpStatusCode.ServiceUnavailable,
                TelegramInitResponse(false, message = "Telegram-логин временно недоступен"))
            return@post
        }

        val nonce = UUID.randomUUID().toString().replace("-", "").take(24)
        val now = LocalDateTime.now()
        val expires = now.plusMinutes(5)

        transaction {
            TelegramAuthSessions.insert {
                it[TelegramAuthSessions.nonce] = nonce
                it[createdAt] = now
                it[expiresAt] = expires
            }
        }

        val deepLink = "https://t.me/${TelegramBotService.botUsername}?start=$nonce"

        call.respond(TelegramInitResponse(
            success = true,
            botUsername = TelegramBotService.botUsername,
            nonce = nonce,
            deepLink = deepLink,
        ))
    }

    /* ── GET /api/auth/telegram/poll?nonce=... ──
     * Client polls every 2-3 sec to check if the user confirmed.
     */
    get("/auth/telegram/poll") {
        val nonce = call.parameters["nonce"]
        if (nonce.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest,
                TelegramPollResponse(false, message = "Missing nonce"))
            return@get
        }

        val session = transaction {
            TelegramAuthSessions.selectAll()
                .where { TelegramAuthSessions.nonce eq nonce }
                .firstOrNull()
        }

        if (session == null) {
            call.respond(HttpStatusCode.NotFound,
                TelegramPollResponse(false, message = "Сессия не найдена"))
            return@get
        }

        if (session[TelegramAuthSessions.expiresAt].isBefore(LocalDateTime.now())) {
            transaction {
                TelegramAuthSessions.deleteWhere { TelegramAuthSessions.nonce eq nonce }
            }
            call.respond(HttpStatusCode.Gone,
                TelegramPollResponse(false, message = "Сессия истекла"))
            return@get
        }

        if (!session[TelegramAuthSessions.confirmed]) {
            call.respond(TelegramPollResponse(success = true, confirmed = false, message = "Ожидание подтверждения…"))
            return@get
        }

        // Confirmed — find or create user
        val telegramId = session[TelegramAuthSessions.telegramId]!!
        val firstName = session[TelegramAuthSessions.firstName] ?: "Пользователь"
        val username = session[TelegramAuthSessions.username]
        val photoUrl = session[TelegramAuthSessions.photoUrl]

        val existingUser = transaction {
            Users.selectAll().where { Users.telegramId eq telegramId }.firstOrNull()
        }

        val (userId, token) = if (existingUser != null) {
            val uid = existingUser[Users.id].toString()
            uid to JwtConfig.generateToken(uid, existingUser[Users.email])
        } else {
            // Create new user
            val email = "tg_${telegramId}@loveu.local"
            val hash = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt(12))
            val uid = transaction {
                Users.insert {
                    it[Users.email] = email
                    it[passwordHash] = hash
                    it[name] = firstName
                    it[Users.telegramId] = telegramId
                    it[avatarUrl] = photoUrl
                    it[createdAt] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }[Users.id].toString()
            }
            uid to JwtConfig.generateToken(uid, email)
        }

        // Clean up session
        transaction {
            TelegramAuthSessions.deleteWhere { TelegramAuthSessions.nonce eq nonce }
        }

        call.respond(TelegramPollResponse(
            success = true,
            confirmed = true,
            token = token,
            userId = userId,
        ))
    }

    /* ── POST /api/telegram/webhook ──
     * Receives Telegram Bot updates (messages + callback queries).
     */
    post("/telegram/webhook") {
        val body = call.receiveText()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val update = json.parseToJsonElement(body).jsonObject

        // Handle /start <nonce> command
        val message = update["message"]?.jsonObject
        if (message != null) {
            val text = message["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val chat = message["chat"]?.jsonObject
            val chatId = chat?.get("id")?.jsonPrimitive?.longOrNull
            val from = message["from"]?.jsonObject
            val tgUserId = from?.get("id")?.jsonPrimitive?.longOrNull

            if (text.startsWith("/start ") && chatId != null && tgUserId != null) {
                val nonce = text.removePrefix("/start ").trim()
                val sessionExists = transaction {
                    TelegramAuthSessions.selectAll()
                        .where { TelegramAuthSessions.nonce eq nonce }
                        .count() > 0
                }
                if (sessionExists) {
                    // Save telegram info and send confirmation button
                    val firstName = from["first_name"]?.jsonPrimitive?.contentOrNull ?: "User"
                    val username = from["username"]?.jsonPrimitive?.contentOrNull

                    transaction {
                        TelegramAuthSessions.update({ TelegramAuthSessions.nonce eq nonce }) {
                            it[TelegramAuthSessions.telegramId] = tgUserId
                            it[TelegramAuthSessions.firstName] = firstName
                            it[TelegramAuthSessions.username] = username
                        }
                    }

                    TelegramBotService.sendAuthConfirmation(chatId, nonce)
                }
            }
        }

        // Handle callback query (user pressed "Confirm" button)
        val callbackQuery = update["callback_query"]?.jsonObject
        if (callbackQuery != null) {
            val data = callbackQuery["data"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val cbId = callbackQuery["id"]?.jsonPrimitive?.contentOrNull.orEmpty()

            if (data.startsWith("confirm_auth:")) {
                val nonce = data.removePrefix("confirm_auth:")
                val from = callbackQuery["from"]?.jsonObject
                val tgUserId = from?.get("id")?.jsonPrimitive?.longOrNull

                if (tgUserId != null) {
                    transaction {
                        TelegramAuthSessions.update({ TelegramAuthSessions.nonce eq nonce }) {
                            it[confirmed] = true
                            it[telegramId] = tgUserId
                        }
                    }
                }

                TelegramBotService.answerCallbackQuery(cbId, "✅ Вход подтверждён!")
            }
        }

        call.respond(HttpStatusCode.OK, "ok")
    }
}

