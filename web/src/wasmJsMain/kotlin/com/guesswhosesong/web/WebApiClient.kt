package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.GWSJson
import com.guesswhosesong.shared.models.SpotifySuggestion
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

@Serializable
data class CreateRoomRequest(val displayName: String)

@Serializable
data class CreateRoomResponse(val joinCode: String)

@Serializable
data class WebSocketTicketResponse(val ticket: String)

@Serializable
data class TracksResponse(val tracks: List<TrackSearchResult> = emptyList())

@Serializable
data class SuggestionsResponse(val suggestions: List<SpotifySuggestion> = emptyList())

@Serializable
data class SpotifyStatusResponse(val connected: Boolean = false)

@Serializable
data class SpotifyAuthUrlResponse(val authorizationUrl: String)

@Serializable
private data class ErrorResponse(val error: String? = null)

class WebApiException(val status: Int, message: String) : Exception(message)

class WebApiClient(private val auth: WebAuthManager) {
    private val baseUrl = configuredApiBaseUrl().trimEnd('/')

    private suspend fun request(path: String, method: String = "GET", body: String = ""): String {
        var response = browserFetch("$baseUrl$path", auth.idToken(), method, body)
        if (response.status == 401) {
            response = browserFetch("$baseUrl$path", auth.idToken(forceRefresh = true), method, body)
        }
        if (response.status !in 200..299) {
            val error = runCatching { GWSJson.decodeFromString<ErrorResponse>(response.body).error }
                .getOrNull()
                ?: "Request failed (${response.status})"
            throw WebApiException(response.status, error)
        }
        return response.body
    }

    suspend fun createRoom(displayName: String): CreateRoomResponse = GWSJson.decodeFromString(
        request("/rooms", "POST", GWSJson.encodeToString(CreateRoomRequest(displayName)))
    )

    suspend fun webSocketTicket(joinCode: String): String =
        GWSJson.decodeFromString<WebSocketTicketResponse>(
            request("/rooms/${joinCode.uppercase()}/ws-ticket", "POST")
        ).ticket

    suspend fun search(query: String): List<TrackSearchResult> =
        GWSJson.decodeFromString<TracksResponse>(request("/music/search?q=${encodeQuery(query)}&limit=20")).tracks

    suspend fun spotifySuggestions(): List<SpotifySuggestion> =
        GWSJson.decodeFromString<SuggestionsResponse>(request("/spotify/suggestions")).suggestions

    suspend fun spotifyStatus(): Boolean =
        GWSJson.decodeFromString<SpotifyStatusResponse>(request("/spotify/status")).connected

    suspend fun spotifyAuthUrl(): String =
        GWSJson.decodeFromString<SpotifyAuthUrlResponse>(request("/spotify/auth-url?client=web")).authorizationUrl

    suspend fun disconnectSpotify() {
        request("/spotify/connection", "DELETE")
    }

    private fun encodeQuery(value: String): String = value.trim().take(200).encodeToByteArray()
        .joinToString("") { byte ->
            val c = byte.toInt() and 0xff
            if (c in 0x30..0x39 || c in 0x41..0x5a || c in 0x61..0x7a || c in listOf(45, 46, 95, 126)) {
                c.toChar().toString()
            } else {
                "%${c.toString(16).padStart(2, '0')}"
            }
        }
}
