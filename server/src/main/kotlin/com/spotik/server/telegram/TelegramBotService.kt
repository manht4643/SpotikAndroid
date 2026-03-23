package com.spotik.server.telegram

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Interacts with Telegram Bot API to:
 * - Send confirmation messages with inline buttons
 * - Set webhooks
 * - Process incoming updates (callback queries)
 */
object TelegramBotService {
    private val log = LoggerFactory.getLogger("TelegramBot")

    private lateinit var botToken: String
    lateinit var botUsername: String
        private set
    var enabled = false
        private set

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun apiUrl(method: String) = "https://api.telegram.org/bot$botToken/$method"

    fun init(config: ApplicationConfig) {
        val tg = try { config.config("telegram") } catch (_: Exception) {
            log.warn("Telegram config not found — Telegram login disabled")
            return
        }
        botToken = tg.propertyOrNull("botToken")?.getString().orEmpty()
        botUsername = tg.propertyOrNull("botUsername")?.getString().orEmpty()
        if (botToken.isBlank() || botUsername.isBlank()) {
            log.warn("Telegram botToken/botUsername missing — Telegram login disabled")
            return
        }
        enabled = true
        log.info("TelegramBotService initialised — bot=@$botUsername")
    }

    /**
     * Set webhook URL so Telegram sends Updates to our server.
     */
    suspend fun setWebhook(webhookUrl: String) {
        if (!enabled) return
        try {
            val resp = client.get(apiUrl("setWebhook")) {
                parameter("url", webhookUrl)
                parameter("allowed_updates", """["message","callback_query"]""")
            }
            log.info("setWebhook response: ${resp.bodyAsText()}")
        } catch (e: Exception) {
            log.error("Failed to set webhook", e)
        }
    }

    /**
     * Send a message to a Telegram user with an inline "Confirm" button.
     *
     * @param chatId  Telegram user chat id (same as user id for private chats)
     * @param nonce   Auth session nonce — embedded in callback_data
     */
    suspend fun sendAuthConfirmation(chatId: Long, nonce: String) {
        if (!enabled) return
        try {
            val keyboard = """
                {"inline_keyboard":[[
                    {"text":"✅ Подтвердить вход в Love u","callback_data":"confirm_auth:$nonce"}
                ]]}
            """.trimIndent()

            client.get(apiUrl("sendMessage")) {
                parameter("chat_id", chatId)
                parameter("text",
                    "🔐 *Запрос авторизации*\n\n" +
                    "Мы получили запрос на авторизацию вашего аккаунта в Telegram на *Love u*.\n\n" +
                    "Чтобы подтвердить, нажмите кнопку ниже.\n" +
                    "Если вы не запрашивали вход — просто проигнорируйте это сообщение.")
                parameter("parse_mode", "Markdown")
                parameter("reply_markup", keyboard)
            }
            log.info("Auth confirmation sent to chatId=$chatId, nonce=$nonce")
        } catch (e: Exception) {
            log.error("Failed to send auth confirmation to $chatId", e)
        }
    }

    /**
     * Answer a callback query (removes the "loading" indicator in Telegram).
     */
    suspend fun answerCallbackQuery(callbackQueryId: String, text: String) {
        if (!enabled) return
        try {
            client.get(apiUrl("answerCallbackQuery")) {
                parameter("callback_query_id", callbackQueryId)
                parameter("text", text)
                parameter("show_alert", false)
            }
        } catch (e: Exception) {
            log.error("Failed to answer callback query", e)
        }
    }
}

