package com.spotik.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Stores temporary Telegram login sessions.
 * Flow: client calls /init → gets nonce → opens Telegram deep link →
 * bot receives /start nonce → sends confirm message → user presses button →
 * bot callback sets telegramId + confirmed = true → client polls /poll?nonce= → gets token.
 */
object TelegramAuthSessions : Table("telegram_auth_sessions") {
    val nonce       = varchar("nonce", 64)
    val telegramId  = long("telegram_id").nullable()
    val firstName   = varchar("first_name", 200).nullable()
    val username    = varchar("username", 200).nullable()
    val photoUrl    = text("photo_url").nullable()
    val confirmed   = bool("confirmed").default(false)
    val createdAt   = datetime("created_at").default(LocalDateTime.now())
    val expiresAt   = datetime("expires_at")

    override val primaryKey = PrimaryKey(nonce)
}

