package com.guesswhosesong.server.routes

import com.guesswhosesong.server.auth.AuthenticatedUser
import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.auth.requireUser
import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.engine.RoomJoinRejectedException
import com.guesswhosesong.server.plugins.RoomNotFoundException
import com.guesswhosesong.server.plugins.UnauthorizedException
import com.guesswhosesong.server.redis.RateLimiter
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.security.InputValidation
import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.JoinRoom
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
private data class CreateRoomRequest(
    val displayName: String,
    val avatarId: String = AvatarCatalog.DEFAULT_ID,
    val avatarCustomization: AvatarCustomization? = null
)

@Serializable
private data class CreateRoomResponse(val joinCode: String)

@Serializable
private data class RoomSummaryResponse(
    val joinCode: String,
    val state: String,
    val playerCount: Int,
    val playerLimit: Int
)

@Serializable
private data class ChatPostRequest(val text: String)

private suspend fun ApplicationCall.requireRoomUser(tokenVerifier: FirebaseTokenVerifier): AuthenticatedUser =
    try {
        requireUser(tokenVerifier)
    } catch (_: Exception) {
        throw UnauthorizedException("AUTH_REQUIRED")
    }

fun Route.roomRoutes(
    roomManager: RoomManager,
    redisClient: RedisClient,
    tokenVerifier: FirebaseTokenVerifier
) {
    val rateLimiter = RateLimiter(redisClient)

    route("/rooms") {
        post {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:create", user.uid, 10, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many room creations"))
                return@post
            }
            if ((call.request.contentLength() ?: 0L) > 8_192L) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Request too large"))
                return@post
            }
            val body = runCatching { call.receive<CreateRoomRequest>() }.getOrNull()
            if (body == null || !InputValidation.displayName(body.displayName)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid room request"))
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
            call.respond(HttpStatusCode.Created, CreateRoomResponse(joinCode = session.room.joinCode))
        }

        get("{joinCode}") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:summary", user.uid, 30, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many room lookups"))
                return@get
            }
            val joinCode = call.roomJoinCode() ?: return@get
            val room = roomManager.findRoom(joinCode)?.room ?: throw RoomNotFoundException(joinCode)
            call.respond(
                RoomSummaryResponse(
                    joinCode = room.joinCode,
                    state = room.state.name,
                    playerCount = room.players.size,
                    playerLimit = room.settings.playerLimit
                )
            )
        }

        post("{joinCode}/join") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:join", user.uid, 30, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
                return@post
            }
            if ((call.request.contentLength() ?: 0L) > 8_192L) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Request too large"))
                return@post
            }
            val joinCode = call.roomJoinCode() ?: return@post
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            val body = runCatching { call.receive<JoinRoom>() }.getOrNull()
            if (body == null || !InputValidation.displayName(body.displayName) ||
                !AvatarCatalog.isValid(body.avatarId) ||
                body.avatarCustomization?.let { !AvatarCustomization.isValid(it) } == true
            ) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid player profile"))
                return@post
            }
            try {
                session.onPlayerConnect(
                    playerId = user.uid,
                    displayName = body.displayName.trim(),
                    avatarId = body.avatarId,
                    avatarCustomization = body.avatarCustomization
                )
            } catch (rejection: RoomJoinRejectedException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to rejection.reason.name))
                return@post
            }
            call.respond(HttpStatusCode.NoContent)
        }

        get("{joinCode}/events") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:events", user.uid, 240, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
                return@get
            }
            val joinCode = call.roomJoinCode() ?: return@get
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            if (!session.hasPlayerConnection(user.uid)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "JOIN_REQUIRED"))
                return@get
            }
            call.respond(session.pollMessages(user.uid, waitMillis = 20_000L))
        }

        get("{joinCode}/chat") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:chat:read", user.uid, 30, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many chat requests"))
                return@get
            }
            val joinCode = call.roomJoinCode() ?: return@get
            val beforeId = call.request.queryParameters["before"]
            if (beforeId != null && beforeId.length !in 1..64) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid message cursor"))
                return@get
            }
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            if (!session.hasPlayerConnection(user.uid)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "JOIN_REQUIRED"))
                return@get
            }
            call.respond(session.chatMessages(beforeId))
        }

        post("{joinCode}/chat") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:chat:write", user.uid, 8, 10)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Slow down before sending another message"))
                return@post
            }
            if ((call.request.contentLength() ?: 0L) > 2_048L) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Message too large"))
                return@post
            }
            val joinCode = call.roomJoinCode() ?: return@post
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            if (!session.hasPlayerConnection(user.uid)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "JOIN_REQUIRED"))
                return@post
            }
            val body = runCatching { call.receive<ChatPostRequest>() }.getOrNull()
            if (body == null || !InputValidation.chatMessage(body.text)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Message must be 1 to 280 characters"))
                return@post
            }
            val message = session.postChatMessage(user.uid, body.text)
            if (message == null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "JOIN_REQUIRED"))
                return@post
            }
            call.respond(HttpStatusCode.Created, message)
        }

        post("{joinCode}/actions") {
            val user = call.requireRoomUser(tokenVerifier)
            if (!rateLimiter.allow("rooms:actions", user.uid, 120, 60)) {
                call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
                return@post
            }
            if ((call.request.contentLength() ?: 0L) > 65_536L) {
                call.respond(HttpStatusCode.PayloadTooLarge, mapOf("error" to "Request too large"))
                return@post
            }
            val joinCode = call.roomJoinCode() ?: return@post
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            if (!session.hasPlayerConnection(user.uid)) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "JOIN_REQUIRED"))
                return@post
            }
            val action = runCatching { call.receive<ClientMessage>() }.getOrNull()
            if (action == null || action is JoinRoom) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid room action"))
                return@post
            }
            session.handleMessage(user.uid, action)
            call.respond(HttpStatusCode.NoContent)
        }

        delete("{joinCode}/join") {
            val user = call.requireRoomUser(tokenVerifier)
            val joinCode = call.roomJoinCode() ?: return@delete
            val session = roomManager.findRoom(joinCode) ?: throw RoomNotFoundException(joinCode)
            session.onPlayerDisconnect(user.uid)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun ApplicationCall.roomJoinCode(): String? {
    val joinCode = parameters["joinCode"]?.uppercase()
    if (joinCode == null) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing joinCode"))
        return null
    }
    if (!InputValidation.joinCode(joinCode)) {
        respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid joinCode"))
        return null
    }
    return joinCode
}
