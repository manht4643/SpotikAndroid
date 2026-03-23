package com.spotik.server.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Core users table — designed for a dating app with fast profile lookups,
 * messaging support (user_id FK), and geospatial readiness (lat/lon columns).
 */
object Users : Table("users") {
    val id            = uuid("id").autoGenerate()
    val email         = varchar("email", 255).uniqueIndex()
    val passwordHash  = varchar("password_hash", 255)
    val name          = varchar("name", 100)
    val age           = integer("age").default(18)
    val city          = varchar("city", 100).default("")
    val bio           = text("bio").default("")
    val avatarUrl     = text("avatar_url").nullable()
    val telegramId    = long("telegram_id").nullable().uniqueIndex()
    val lat           = double("lat").nullable()           // for future geo queries
    val lon           = double("lon").nullable()
    val isVerified    = bool("is_verified").default(false)
    val isPremium     = bool("is_premium").default(false)
    val createdAt     = datetime("created_at").default(LocalDateTime.now())
    val updatedAt     = datetime("updated_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

