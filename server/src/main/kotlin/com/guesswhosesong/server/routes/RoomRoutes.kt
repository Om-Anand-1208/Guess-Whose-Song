package com.guesswhosesong.server.routes

import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.auth.requireUser
import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.plugins.RoomNotFoundException
import com.guesswhosesong.server.plugins.UnauthorizedException
import com.guesswhosesong.server.redis.RateLimiter
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.redis.WsTicketRepository
import com.guesswhosesong.server.security.InputValidation
import com.guesswhosesong.server.security.WebSocketAuthentication
import com.guesswhosesong.shared.dto.JoinRoom
import com.guesswhosesong.shared.dto.toClientMessage
import com.guesswhosesong.shared.models.AvatarCustomization
import com.guesswhosesong.shared.models.AvatarCatalog
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class CreateRoomRequest(
    val displayName: String,
    val avatarId: String = AvatarCatalog.DEFAULT_ID,
    val avatarCustomization: AvatarCustomization? = null
)

@Serializable
private data class CreateRoomResponse(
    val joinCode: String
)

@Serializable
private data class RoomSummaryResponse(
    val joinCode: String,
    val state: String,
    val playerCount: Int,
    val playerLimit: Int
)

fun Route.roomRoutes(
    roomManager: RoomManager,
    redisClient: RedisClient,
    tokenVerifier: FirebaseTokenVerifier
) {
    val rateLimiter = RateLimiter(redisClient)
    val ticketRepository = WsTicketRepository(redisClient)

    route("/rooms") {

        /**
         * POST /rooms
         * Body: { displayName: String, avatarId: String, avatarCustomization?: AvatarCustomization }
         * Header: Authorization: Bearer <Firebase ID token>
         * Creates a new room and returns the join code.
         */
        post {
            val user = try {
                call.requireUser(tokenVerifier)
            } catch (_: Exception) {
                throw UnauthorizedException("AUTH_REQUIRED")
            }
            if (!rateLimiter.allow("rooms:create", user.uid, 10, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many room creations"))
                return@post
            }

            if ((call.request.contentLength() ?: 0L) > 8_192L) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Request too large"))
                return@post
            }
            val body = try {
                call.receive<CreateRoomRequest>()
            } catch (_: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request"))
                return@post
            }
            if (!InputValidation.displayName(body.displayName)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "displayName must be 1-24 characters"))
                return@post
            }
            if (body.avatarCustomization?.let { !AvatarCustomization.isValid(it) } == true) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid avatarCustomization"))
                return@post
            }

            val session = roomManager.createRoom(
                hostId = user.uid,
                hostName = body.displayName.trim(),
                hostAvatarId = AvatarCatalog.normalize(body.avatarId),
                hostAvatarCustomization = body.avatarCustomization
            )
            call.respond(
                HttpStatusCode.Created,
                CreateRoomResponse(joinCode = session.room.joinCode)
            )
        }

        /**
         * GET /rooms/{joinCode}
         * Returns basic room info (player count, state) for lobby preview.
         * Requires Firebase authentication; summaries are rate-limited.
         */
        get("{joinCode}") {
            val user = try {
                call.requireUser(tokenVerifier)
            } catch (_: Exception) {
                throw UnauthorizedException("AUTH_REQUIRED")
            }
            if (!rateLimiter.allow("rooms:summary", user.uid, 30, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many room lookups"))
                return@get
            }
            val joinCode = call.parameters["joinCode"]?.uppercase()
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing joinCode"))
            if (!InputValidation.joinCode(joinCode)) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid joinCode"))
            }
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
         * POST /rooms/{joinCode}/ws-ticket
         * Creates a 30-second, single-use browser WebSocket credential. Native
         * browser WebSockets cannot set Authorization headers, so the ticket is
         * negotiated as a WebSocket subprotocol and never placed in a URL.
         */
        post("{joinCode}/ws-ticket") {
            val user = try {
                call.requireUser(tokenVerifier)
            } catch (_: Exception) {
                throw UnauthorizedException("AUTH_REQUIRED")
            }
            if (!rateLimiter.allow("ws:ticket", user.uid, 30, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
                return@post
            }
            val joinCode = call.parameters["joinCode"]?.uppercase()
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing joinCode"))
            if (!InputValidation.joinCode(joinCode)) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid joinCode"))
            }
            if (roomManager.findRoom(joinCode) == null) {
                throw RoomNotFoundException(joinCode)
            }
            call.respond(mapOf("ticket" to ticketRepository.create(user.uid, joinCode)))
        }

        /**
         * Browser connections offer the fixed marker plus the short-lived
         * ticket protocol. Ktor can negotiate the fixed marker before the
         * upgrade while the handler reads the ticket from the request header.
         */
        webSocket("{joinCode}/ws", protocol = WebSocketAuthentication.ROUTE_PROTOCOL) {
            handleRoomWebSocket(roomManager, rateLimiter, ticketRepository, tokenVerifier)
        }

        /** Native clients keep using the Authorization-header variant. */
        webSocket("{joinCode}/ws") {
            handleRoomWebSocket(roomManager, rateLimiter, ticketRepository, tokenVerifier)
        }
    }
}

private suspend fun DefaultWebSocketServerSession.handleRoomWebSocket(
    roomManager: RoomManager,
    rateLimiter: RateLimiter,
    ticketRepository: WsTicketRepository,
    tokenVerifier: FirebaseTokenVerifier
) {
    val joinCode = call.parameters["joinCode"]?.uppercase()
        ?: run {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing joinCode"))
            return
        }
    if (!InputValidation.joinCode(joinCode)) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid joinCode"))
        return
    }

    val offeredProtocol = WebSocketAuthentication.offeredTicketProtocol(
        call.request.headers.getAll(HttpHeaders.SecWebSocketProtocol)?.joinToString(",")
    )
    val user = try {
        val authorization = call.request.headers[HttpHeaders.Authorization]
        if (!authorization.isNullOrBlank()) {
            call.requireUser(tokenVerifier)
        } else {
            val ticket = ticketRepository.consume(
                WebSocketAuthentication.ticketFromProtocol(offeredProtocol)
                    ?: throw IllegalArgumentException("AUTH_REQUIRED")
            ) ?: throw IllegalArgumentException("AUTH_REQUIRED")
            if (ticket.joinCode != joinCode) throw IllegalArgumentException("ROOM_MISMATCH")
            com.guesswhosesong.server.auth.AuthenticatedUser(ticket.uid, isAnonymous = false)
        }
    } catch (_: Exception) {
        close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "AUTH_REQUIRED"))
        return
    }

    val session = roomManager.findRoom(joinCode)
    if (session == null) {
        application.log.error("Room not found for code: $joinCode")
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room not found"))
        return
    }

    val room = session.room
    val isNewPlayer = room.players.none { it.id == user.uid }
    if (isNewPlayer && room.players.size >= room.settings.playerLimit) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Room is full"))
        return
    }
    if (isNewPlayer && room.state != com.guesswhosesong.shared.models.RoomState.LOBBY) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Game already started"))
        return
    }

    var registered = false
    val connectionId = UUID.randomUUID().toString()

    try {
        val firstMessage = withTimeout(10_000L) { incoming.receiveCatching().getOrNull() }
            ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "JOIN_REQUIRED"))
                return
            }
        val joinMessage = try {
            (firstMessage as? Frame.Text)?.readText()?.toClientMessage()
                ?: throw IllegalArgumentException("JOIN_REQUIRED")
        } catch (_: Exception) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "JOIN_REQUIRED"))
            return
        }
        if (joinMessage !is JoinRoom ||
            !InputValidation.displayName(joinMessage.displayName) ||
            !AvatarCatalog.isValid(joinMessage.avatarId) ||
            joinMessage.avatarCustomization?.let { !AvatarCustomization.isValid(it) } == true
        ) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "JOIN_REQUIRED"))
            return
        }

        session.onPlayerConnect(
            playerId = user.uid,
            displayName = joinMessage.displayName.trim(),
            avatarId = joinMessage.avatarId,
            socket = this,
            avatarCustomization = joinMessage.avatarCustomization
        )
        registered = true

        for (frame in incoming) {
            if (frame is Frame.Text) {
                if (!rateLimiter.allow("ws:messages", connectionId, 20, 1)) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "RATE_LIMITED"))
                    break
                }
                try {
                    val message = frame.readText().toClientMessage()
                    if (message is JoinRoom) {
                        application.log.warn("Repeated JOIN_ROOM rejected")
                        continue
                    }
                    session.handleMessage(user.uid, message)
                } catch (_: Exception) {
                    application.log.warn("Invalid WS message")
                }
            }
        }
    } finally {
        if (registered) session.onPlayerDisconnect(user.uid, this)
    }
}
