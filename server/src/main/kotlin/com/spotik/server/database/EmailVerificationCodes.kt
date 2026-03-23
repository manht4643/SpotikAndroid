package com.spotik.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Stores 6-digit OTP codes for email verification.
 * Each row is upserted when a new code is requested for the same email.
 */
object EmailVerificationCodes : Table("email_verification_codes") {
    val email     = varchar("email", 255)
    val code      = varchar("code", 6)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(email)
}

