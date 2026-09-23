package com.guesswhosesong.server.routes

import com.guesswhosesong.server.firebase.FirebaseAdmin
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.spotify.SpotifyClient
import com.guesswhosesong.server.spotify.SpotifyOAuth
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private val spotifyOAuth = SpotifyOAuth()
private val spotifyClient = SpotifyClient()
private val json = Json { ignoreUnknownKeys = true }

// Spotify tokens stored in Redis per player: "spotify_token:{playerId}"
private const val TOKEN_TTL = 3600L // 1 hour

fun Route.spotifyRoutes(redis: RedisClient) {
    route("/spotify") {

        /**
         * GET /spotify/auth?state=<firebaseUid>
         * Redirects the user to Spotify's authorization page.
         */
        get("/auth") {
            if (!spotifyOAuth.isConfigured) {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Spotify not configured"))
                return@get
            }
            val state = call.request.queryParameters["state"] ?: ""
            call.respondRedirect(spotifyOAuth.buildAuthUrl(state))
        }

        /**
         * GET /spotify/callback?code=<authCode>&state=<firebaseUid>
         * Exchanges code for token, stores in Redis, redirects back to Android app.
         */
        get("/callback") {
            val code = call.request.queryParameters["code"]
            val playerId = call.request.queryParameters["state"]
            if (code == null || playerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing code or state"))
                return@get
            }
            try {
                val tokenResponse = spotifyOAuth.exchangeCode(code)
                redis.set("spotify_token:$playerId", tokenResponse.accessToken, TOKEN_TTL.toLong())
                // Redirect back to the Android app deep link
                call.respondRedirect("guesswhosesong://spotify-callback?success=true")
            } catch (e: Exception) {
                call.respondRedirect("guesswhosesong://spotify-callback?success=false")
            }
        }

        /** Helper to extract and verify the Bearer token from the Authorization header */
        fun ApplicationCall.playerIdFromToken(): String? {
            val token = request.authorization()?.removePrefix("Bearer ") ?: return null
            return try { FirebaseAdmin.verifyIdToken(token) } catch (e: Exception) { null }
        }

        fun ApplicationCall.spotifyToken(playerId: String): String? =
            redis.get("spotify_token:$playerId")

        /**
         * GET /spotify/top
         * Returns the player's top tracks from Spotify.
         */
        get("/top") {
            val playerId = call.playerIdFromToken()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            val token = call.spotifyToken(playerId)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Spotify not connected"))
            val tracks = spotifyClient.getTopTracks(token)
            call.respond(mapOf("tracks" to tracks))
        }

        /**
         * GET /spotify/recent
         */
        get("/recent") {
            val playerId = call.playerIdFromToken()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            val token = call.spotifyToken(playerId)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Spotify not connected"))
            val tracks = spotifyClient.getRecentlyPlayed(token)
            call.respond(mapOf("tracks" to tracks))
        }

        /**
         * GET /spotify/playlists/{playlistId}/tracks
         */
        get("/playlists/{playlistId}/tracks") {
            val playerId = call.playerIdFromToken()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
            val token = call.spotifyToken(playerId)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Spotify not connected"))
            val playlistId = call.parameters["playlistId"] ?: ""
            val tracks = spotifyClient.getPlaylistTracks(token, playlistId)
            call.respond(mapOf("tracks" to tracks))
        }
    }
}
