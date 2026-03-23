package com.spotik.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.server.config.*
import java.util.*

object JwtConfig {
    private lateinit var secret: String
    private lateinit var issuer: String
    private lateinit var audience: String
    lateinit var realm: String
        private set
    private var expiresInMs: Long = 604_800_000L // 7 days default

    fun init(config: ApplicationConfig) {
        val jwtConfig = config.config("jwt")
        secret = jwtConfig.property("secret").getString()
        issuer = jwtConfig.property("issuer").getString()
        audience = jwtConfig.property("audience").getString()
        realm = jwtConfig.property("realm").getString()
        expiresInMs = jwtConfig.propertyOrNull("expiresInMs")?.getString()?.toLongOrNull() ?: 604_800_000L
    }

    val verifier: JWTVerifier by lazy {
        JWT.require(Algorithm.HMAC256(secret))
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
    }

    fun generateToken(userId: String, email: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMs))
            .sign(Algorithm.HMAC256(secret))
}

