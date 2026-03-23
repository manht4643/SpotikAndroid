package com.spotik.server

import com.spotik.server.database.DatabaseFactory
import com.spotik.server.plugins.configureAuth
import com.spotik.server.plugins.configureRouting
import com.spotik.server.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.response.*
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Database
    DatabaseFactory.init(environment.config)

    // Plugins
    install(CallLogging) { level = Level.INFO }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("success" to false, "message" to (cause.localizedMessage ?: "Internal error"))
            )
        }
    }

    configureSerialization()
    configureAuth()
    configureRouting()
}

