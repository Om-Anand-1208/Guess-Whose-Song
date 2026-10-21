package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.di.NetworkModule
import com.guesswhosesong.shared.models.RoomChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class ChatPostRequest(val text: String)

class RoomChatRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val identityManager: PlayerIdentityManager
) {
    suspend fun recent(joinCode: String, beforeId: String? = null): List<RoomChatMessage> {
        val response = httpClient.get("${NetworkModule.BASE_URL}/rooms/$joinCode/chat") {
            bearerAuth(identityManager.getIdToken())
            if (beforeId != null) url { parameters.append("before", beforeId) }
        }
        check(response.status.isSuccess()) { "Chat is unavailable" }
        return response.body()
    }

    suspend fun send(joinCode: String, text: String): RoomChatMessage {
        val response = httpClient.post("${NetworkModule.BASE_URL}/rooms/$joinCode/chat") {
            bearerAuth(identityManager.getIdToken())
            contentType(ContentType.Application.Json)
            setBody(ChatPostRequest(text))
        }
        check(response.status.isSuccess()) { "Could not send message" }
        return response.body()
    }
}
