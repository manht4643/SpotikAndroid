package com.spotik.server.routes

import com.spotik.server.database.EmailVerificationCodes
import com.spotik.server.email.EmailService
import com.spotik.server.models.ApiResponse
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import kotlin.random.Random

@Serializable
data class SendCodeRequest(val email: String)

@Serializable
data class VerifyCodeRequest(val email: String, val code: String)

fun Route.emailVerificationRoutes() {

    /* ═══════  POST /api/email/send-code  ═══════
     * Generate a 6-digit code, store it with 5-min TTL, send via email.
     */
    post("/email/send-code") {
        val req = call.receive<SendCodeRequest>()
        val email = req.email.lowercase().trim()

        if (email.isBlank() || !email.contains("@")) {
            call.respond(HttpStatusCode.BadRequest,
                ApiResponse(false, "Некорректный email"))
            return@post
        }

        // Rate-limit: don't send a new code if the last one was sent < 30 s ago
        val recentCode = transaction {
            EmailVerificationCodes.selectAll()
                .where { EmailVerificationCodes.email eq email }
                .firstOrNull()
        }
        if (recentCode != null) {
            val created = recentCode[EmailVerificationCodes.createdAt]
            if (created.plusSeconds(30).isAfter(LocalDateTime.now())) {
                call.respond(HttpStatusCode.TooManyRequests,
                    ApiResponse(false, "Подождите 30 секунд перед повторной отправкой"))
                return@post
            }
        }

        val code = "%06d".format(Random.nextInt(100_000, 1_000_000))
        val now = LocalDateTime.now()
        val expires = now.plusMinutes(5)

        // Upsert
        transaction {
            if (recentCode != null) {
                EmailVerificationCodes.update({ EmailVerificationCodes.email eq email }) {
                    it[EmailVerificationCodes.code] = code
                    it[createdAt] = now
                    it[expiresAt] = expires
                }
            } else {
                EmailVerificationCodes.insert {
                    it[EmailVerificationCodes.email] = email
                    it[EmailVerificationCodes.code] = code
                    it[createdAt] = now
                    it[expiresAt] = expires
                }
            }
        }

        val sent = EmailService.sendVerificationCode(email, code)
        if (sent) {
            call.respond(ApiResponse(true, "Код отправлен на $email"))
        } else {
            call.respond(HttpStatusCode.InternalServerError,
                ApiResponse(false, "Не удалось отправить письмо, попробуйте позже"))
        }
    }

    /* ═══════  POST /api/email/verify-code  ═══════
     * Check that the code matches and hasn't expired.
     */
    post("/email/verify-code") {
        val req = call.receive<VerifyCodeRequest>()
        val email = req.email.lowercase().trim()

        val row = transaction {
            EmailVerificationCodes.selectAll()
                .where { EmailVerificationCodes.email eq email }
                .firstOrNull()
        }

        if (row == null) {
            call.respond(HttpStatusCode.BadRequest,
                ApiResponse(false, "Код не запрашивался для этого email"))
            return@post
        }

        if (row[EmailVerificationCodes.expiresAt].isBefore(LocalDateTime.now())) {
            transaction {
                EmailVerificationCodes.deleteWhere { EmailVerificationCodes.email eq email }
            }
            call.respond(HttpStatusCode.Gone,
                ApiResponse(false, "Код истёк, запросите новый"))
            return@post
        }

        if (row[EmailVerificationCodes.code] != req.code.trim()) {
            call.respond(HttpStatusCode.Unauthorized,
                ApiResponse(false, "Неверный код"))
            return@post
        }

        // Success — delete used code
        transaction {
            EmailVerificationCodes.deleteWhere { EmailVerificationCodes.email eq email }
        }

        call.respond(ApiResponse(true, "Email подтверждён"))
    }
}

