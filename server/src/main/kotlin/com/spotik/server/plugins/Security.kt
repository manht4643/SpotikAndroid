package com.spotik.server.plugins

import com.spotik.server.auth.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureAuth() {
    JwtConfig.init(environment.config)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verifier)
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()
                if (!userId.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }
    }
}

/** Extension to quickly get userId from JWT principal. */
fun JWTPrincipal.userId(): String = payload.getClaim("userId").asString()
fun JWTPrincipal.email(): String = payload.getClaim("email").asString()

