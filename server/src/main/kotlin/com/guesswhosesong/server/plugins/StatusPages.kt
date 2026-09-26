package com.guesswhosesong.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class RoomNotFoundException(joinCode: String) : Exception("Room not found: $joinCode")
class UnauthorizedException(message: String = "Unauthorized") : Exception(message)
class GameStateException(message: String) : Exception(message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RoomNotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("error" to cause.message))
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to cause.message))
        }
        exception<GameStateException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error(
                "Unhandled request exception: ${cause::class.simpleName}"
            )
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to "Internal server error")
            )
        }
    }
}
