package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.di.NetworkModule
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.shared.dto.GWSJson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.headers
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomResponse(
    val joinCode: String
)

@Serializable
data class RoomInfoResponse(
    val joinCode: String,
    val state: String,
    val playerCount: Int,
    val playerLimit: Int
)

/**
 * HTTP repository for room management REST calls.
 */
class RoomRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val identityManager: PlayerIdentityManager
) {

    /**
     * Creates a new room. Returns the join code.
     */
    suspend fun createRoom(displayName: String, avatarId: String): Result<CreateRoomResponse> {
        return runCatching {
            val idToken = identityManager.getIdToken()
            httpClient.post("$baseUrl/rooms") {
                contentType(ContentType.Application.Json)
                bearerAuth(idToken)
                setBody(mapOf("displayName" to displayName, "avatarId" to avatarId))
            }.body<CreateRoomResponse>()
        }
    }

    /**
     * Fetches basic room info for lobby preview.
     */
    suspend fun getRoomInfo(joinCode: String): Result<RoomInfoResponse> {
        return runCatching {
            httpClient.get("$baseUrl/rooms/${joinCode.uppercase()}") {
                bearerAuth(identityManager.getIdToken())
            }.body<RoomInfoResponse>()
        }
    }
}
