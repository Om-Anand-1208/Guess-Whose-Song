package com.guesswhosesong.server.spotify

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

@Serializable
data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresIn: Int = 3600,
    @SerialName("token_type") val tokenType: String = "Bearer"
)

/**
 * Server-side Spotify OAuth helper.
 * The Android client initiates PKCE OAuth via a Custom Tab pointing to the server's
 * /spotify/auth endpoint. The server completes the code exchange and returns the token.
 */
class SpotifyOAuth(
    private val clientId: String = System.getenv("SPOTIFY_CLIENT_ID") ?: "",
    private val clientSecret: String = System.getenv("SPOTIFY_CLIENT_SECRET") ?: "",
    private val redirectUri: String = System.getenv("SPOTIFY_REDIRECT_URI") ?: ""
) {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    val isConfigured get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    fun buildAuthUrl(state: String): String {
        val scopes = listOf(
            "user-top-read",
            "user-read-recently-played",
            "playlist-read-private"
        ).joinToString(" ")
        return "https://accounts.spotify.com/authorize?" +
            "response_type=code" +
            "&client_id=$clientId" +
            "&scope=${Uri.encode(scopes)}" +
            "&redirect_uri=${Uri.encode(redirectUri)}" +
            "&state=$state"
    }

    suspend fun exchangeCode(code: String): SpotifyTokenResponse {
        val credentials = Base64.getEncoder()
            .encodeToString("$clientId:$clientSecret".toByteArray())
        return httpClient.post("https://accounts.spotify.com/api/token") {
            header(HttpHeaders.Authorization, "Basic $credentials")
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
            }))
        }.body()
    }

    fun close() = httpClient.close()

    // Simple URL encode helper (avoids importing android URI on server)
    private object Uri {
        fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
    }
}
