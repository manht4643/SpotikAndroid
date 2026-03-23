package com.spotik.server.routes

import com.spotik.server.database.Users
import com.spotik.server.models.*
import com.spotik.server.plugins.userId
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.*

fun Route.profileRoutes() {

    authenticate("auth-jwt") {

        /* ═══════  GET /api/me  ═══════ */
        get("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val uid = try { UUID.fromString(principal.userId()) } catch (_: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid token"))
                return@get
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq uid }.firstOrNull()
            }

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Пользователь не найден"))
                return@get
            }

            call.respond(UserProfile(
                id = user[Users.id].toString(),
                name = user[Users.name],
                age = user[Users.age],
                city = user[Users.city],
                bio = user[Users.bio],
                avatarUrl = user[Users.avatarUrl],
                email = user[Users.email],
            ))
        }

        /* ═══════  PUT /api/me  ═══════ */
        put("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val uid = try { UUID.fromString(principal.userId()) } catch (_: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid token"))
                return@put
            }

            val req = call.receive<UpdateProfileRequest>()

            val updated = transaction {
                Users.update({ Users.id eq uid }) {
                    req.name?.let { v -> it[name] = v }
                    req.age?.let { v -> it[age] = v.coerceIn(14, 99) }
                    req.bio?.let { v -> it[bio] = v }
                    req.city?.let { v -> it[city] = v }
                    req.avatarUrl?.let { v -> it[avatarUrl] = v }
                    it[updatedAt] = LocalDateTime.now()
                }
            }

            if (updated > 0) {
                call.respond(ApiResponse(true, "Профиль обновлён"))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Пользователь не найден"))
            }
        }
    }
}

