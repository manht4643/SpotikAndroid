package com.spotik.server.plugins

import com.spotik.server.routes.authRoutes
import com.spotik.server.routes.profileRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(mapOf("status" to "ok", "service" to "Spotik API", "version" to "1.0.0"))
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }

        route("/api") {
            authRoutes()
            profileRoutes()
        }
    }
}

