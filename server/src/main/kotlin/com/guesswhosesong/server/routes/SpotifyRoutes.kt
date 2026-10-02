package com.guesswhosesong.server.routes

import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.auth.requireUser
import com.guesswhosesong.server.redis.RateLimiter
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.security.InputValidation
import com.guesswhosesong.server.spotify.SpotifyClient
import com.guesswhosesong.server.spotify.SpotifyOAuth
import com.guesswhosesong.shared.models.SpotifySuggestion
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private const val TOKEN_PREFIX = "spotify_token:"
private const val OAUTH_PREFIX = "spotify_oauth:"
private const val TOKEN_TTL_SECONDS = 3600L
private const val OAUTH_TTL_SECONDS = 600L
private const val ANDROID_CLIENT = "android"
private const val WEB_CLIENT = "web"
private val spotifyOAuth = SpotifyOAuth()
private val secureRandom = SecureRandom()

fun closeSpotifyResources() {
    spotifyOAuth.close()
}

fun Route.spotifyRoutes(
    redis: RedisClient,
    tokenVerifier: FirebaseTokenVerifier,
    spotifyClient: SpotifyClient
) {
    val rateLimiter = RateLimiter(redis)

    fun oauthStateKey(state: String) = "$OAUTH_PREFIX$state"
    fun tokenKey(uid: String) = "$TOKEN_PREFIX$uid"
    fun randomUrlSafe(bytes: Int): String {
        val value = ByteArray(bytes).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value)
    }
    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

    suspend fun ApplicationCall.authenticatedUser() =
        try { requireUser(tokenVerifier) } catch (_: Exception) { null }

    route("/spotify") {
        get("/auth-url") {
            val user = call.authenticatedUser()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
            if (!spotifyOAuth.isConfigured) {
                return@get call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Spotify not configured"))
            }
            val client = call.request.queryParameters["client"]?.trim()?.lowercase() ?: ANDROID_CLIENT
            if (client !in setOf(ANDROID_CLIENT, WEB_CLIENT)) {
                return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid OAuth client"))
            }
            val webRedirectUri = System.getenv("SPOTIFY_WEB_REDIRECT_URI").orEmpty()
            if (client == WEB_CLIENT && webRedirectUri.isBlank()) {
                return@get call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Web Spotify redirect is not configured"))
            }
            val state = randomUrlSafe(32)
            val verifier = randomUrlSafe(48)
            val encodedUid = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.uid.toByteArray(Charsets.UTF_8))
            if (!redis.setIfAbsent(oauthStateKey(state), "$encodedUid:$verifier:$client", OAUTH_TTL_SECONDS)) {
                return@get call.respond(HttpStatusCode.ServiceUnavailable, mapOf("error" to "Could not start Spotify authorization"))
            }
            call.respond(mapOf("authorizationUrl" to spotifyOAuth.buildAuthUrl(state, challenge(verifier))))
        }

        get("/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            if (code.isNullOrBlank() || state.isNullOrBlank() || code.length > 2_000 || state.length > 200) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid OAuth callback"))
                return@get
            }
            // Delete before exchange: a callback can only be used once, including retries.
            val rawState = redis.get(oauthStateKey(state))
            redis.del(oauthStateKey(state))
            val parts = rawState?.split(":", limit = 3)
            val uid = parts?.firstOrNull()?.let {
                runCatching { String(Base64.getUrlDecoder().decode(it), Charsets.UTF_8) }.getOrNull()
            }
            val client = parts?.getOrNull(2)
            if (parts == null || parts.size != 3 || uid.isNullOrBlank() || parts[1].isBlank() ||
                client !in setOf(ANDROID_CLIENT, WEB_CLIENT)
            ) {
                call.respondRedirect("guesswhosesong://spotify-callback?success=false")
                return@get
            }
            try {
                val response = spotifyOAuth.exchangeCode(code, parts[1])
                val ttl = response.expiresIn.toLong().coerceIn(60L, TOKEN_TTL_SECONDS)
                redis.set(tokenKey(uid), response.accessToken, ttl)
                if (client == WEB_CLIENT) {
                    val redirect = System.getenv("SPOTIFY_WEB_REDIRECT_URI").orEmpty()
                    val separator = if ('?' in redirect) '&' else '?'
                    call.respondRedirect("$redirect${separator}spotify=callback")
                } else {
                    call.respondRedirect("guesswhosesong://spotify-callback?success=true")
                }
            } catch (_: Exception) {
                if (client == WEB_CLIENT) {
                    val redirect = System.getenv("SPOTIFY_WEB_REDIRECT_URI").orEmpty()
                    val separator = if ('?' in redirect) '&' else '?'
                    call.respondRedirect("$redirect${separator}spotify=callback")
                } else {
                    call.respondRedirect("guesswhosesong://spotify-callback?success=false")
                }
            }
        }

        get("/status") {
            val user = call.authenticatedUser()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
            call.respond(mapOf("connected" to (redis.get(tokenKey(user.uid)) != null)))
        }

        delete("/connection") {
            val user = call.authenticatedUser()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
            redis.del(tokenKey(user.uid))
            call.respond(mapOf("connected" to false))
        }

        get("/top") {
            call.respondSpotify(redis, tokenVerifier, rateLimiter, spotifyClient, "spotify:top") {
                spotifyClient.getTopTracks(it)
            }
        }
        get("/recent") {
            call.respondSpotify(redis, tokenVerifier, rateLimiter, spotifyClient, "spotify:recent") {
                spotifyClient.getRecentlyPlayed(it)
            }
        }

        get("/playlists/{playlistId}/tracks") {
            val playlistId = call.parameters["playlistId"]
            if (playlistId == null || !InputValidation.playlistId(playlistId)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid playlist ID"))
                return@get
            }
            call.respondSpotify(redis, tokenVerifier, rateLimiter, spotifyClient, "spotify:playlist") {
                spotifyClient.getPlaylistTracks(it, playlistId)
            }
        }

        get("/suggestions") {
            call.respondSpotify(
                redis,
                tokenVerifier,
                rateLimiter,
                spotifyClient,
                "spotify:suggestions",
                responseKey = "suggestions"
            ) { spotifyClient.getTopTracks(it) }
        }
    }
}

private suspend fun ApplicationCall.respondSpotify(
    redis: RedisClient,
    verifier: FirebaseTokenVerifier,
    rateLimiter: RateLimiter,
    client: SpotifyClient,
    scope: String,
    responseKey: String = "tracks",
    operation: suspend (String) -> List<SpotifySuggestion>
) {
    val user = try { requireUser(verifier) } catch (_: Exception) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
        return
    }
    if (!rateLimiter.allow(scope, user.uid, 30, 60)) {
        respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
        return
    }
    val key = "spotify_token:${user.uid}"
    val token = redis.get(key)
    if (token == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Spotify not connected"))
        return
    }
    try {
        respond(mapOf(responseKey to operation(token)))
    } catch (_: SpotifyClient.UnauthorizedException) {
        redis.del(key)
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Spotify authorization expired"))
    }
}
