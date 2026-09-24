package com.guesswhosesong.server.routes

import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.plugins.RoomNotFoundException
import com.guesswhosesong.server.plugins.UnauthorizedException
import com.guesswhosesong.shared.dto.toClientMessage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable

@Serializable
private data class CreateRoomRequest(
    val displayName: String
)

@Serializable
private data class CreateRoomResponse(
    val joinCode: String,
    val playerId: String
)

@Serializable
private data class RoomSummaryResponse(
    val joinCode: String,
    val state: String,
    val playerCount: Int,
    val playerLimit: Int
)

fun Route.roomRoutes(roomManager: RoomManager) {

    route("/rooms") {

        /**
         * POST /rooms
         * Body: { displayName: String }
         * Header: Authorization: Bearer <playerId>
         * Creates a new room and returns the join code.
         */
        post {
            val token = call.request.authorization()?.removePrefix("Bearer ")?.trim()
                ?: throw UnauthorizedException("Missing Authorization header")
            if (token.isBlank()) {
                throw UnauthorizedException("Invalid token")
            }
            val playerId = token

            val body = call.receive<CreateRoomRequest>()
            if (body.displayName.isBlank() || body.displayName.length > 24) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "displayName must be 1-24 characters"))
                return@post
            }

            val session = roomManager.createRoom(playerId, body.displayName.trim())
            call.respond(
                HttpStatusCode.Created,
                CreateRoomResponse(joinCode = session.room.joinCode, playerId = playerId)
            )
        }

        /**
         * GET /rooms/{joinCode}
         * Returns basic room info (player count, state) for lobby preview.
         * No auth required.
         */
        get("{joinCode}") {
            val joinCode = call.parameters["joinCode"]?.uppercase()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing joinCode"))
            val session = roomManager.findRoom(joinCode)
                ?: throw RoomNotFoundException(joinCode)
            val room = session.room
            call.respond(
                RoomSummaryResponse(
                    joinCode = room.joinCode,
                    state = room.state.name,
                    playerCount = room.players.size,
                    playerLimit = room.settings.playerLimit
                )
            )
        }

        /**
         * WS /rooms/{joinCode}/ws?token=<playerId>&displayName=<name>
         * Upgrades to WebSocket. The token is used to identify the player.
         */
        webSocket("{joinCode}/ws") {
            val joinCode = call.parameters["joinCode"]?.uppercase()
                ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing joinCode"))
            val token = call.request.queryParameters["token"]?.trim()
                ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing token"))
            val displayName = call.request.queryParameters["displayName"]?.trim()?.take(24)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing displayName"))

            val playerId = if (token.isNotBlank()) token else {
                application.log.error("WebSocket connection rejected: blank token")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid token"))
                return@webSocket
            }

            // Find or reject room
            val session = roomManager.findRoom(joinCode)
            if (session == null) {
                application.log.error("Room not found for code: $joinCode")
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room not found"))
                return@webSocket
            }

            // Check player limit
            val room = session.room
            val isNewPlayer = room.players.none { it.id == playerId }
            if (isNewPlayer && room.players.size >= room.settings.playerLimit) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
                return@webSocket
            }
            // Can't join a game already in progress
            if (isNewPlayer && room.state != com.guesswhosesong.shared.models.RoomState.LOBBY) {
                close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game already started"))
                return@webSocket
            }

            // Register connection
            session.onPlayerConnect(playerId, displayName, this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        try {
                            val message = frame.readText().toClientMessage()
                            session.handleMessage(playerId, message)
                        } catch (e: Exception) {
                            application.log.warn("Invalid WS message from $playerId: ${e.message}")
                        }
                    }
                }
            } finally {
                session.onPlayerDisconnect(playerId)
            }
        }
    }
}
