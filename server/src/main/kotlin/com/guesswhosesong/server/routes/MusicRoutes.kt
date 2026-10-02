package com.guesswhosesong.server.routes

import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.auth.requireUser
import com.guesswhosesong.server.music.MusicService
import com.guesswhosesong.server.redis.RateLimiter
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.security.InputValidation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Proxy route for Deezer-first music search with iTunes fallback.
 * Clients call this instead of music APIs directly (avoids CORS, encapsulates API).
 *
 * GET /music/search?q=<query>&limit=<n>
 * GET /music/top
 */
fun Route.musicRoutes(
    tokenVerifier: FirebaseTokenVerifier,
    redisClient: RedisClient,
    musicService: MusicService
) {
    val rateLimiter = RateLimiter(redisClient)
    route("/music") {
        get("/search") {
            val user = call.requireUserOrNull(tokenVerifier)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
            if (!rateLimiter.allow("music:search", user.uid, 30, 60)) {
                return@get call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
            }
            val query = call.request.queryParameters["q"]?.trim()
            if (query == null || !InputValidation.searchTerm(query)) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing query parameter 'q'"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
            val results = musicService.search(query, limit)
            call.respond(mapOf("tracks" to results))
        }

        get("/top") {
            val user = call.requireUserOrNull(tokenVerifier)
                ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "AUTH_REQUIRED"))
            if (!rateLimiter.allow("music:top", user.uid, 30, 60)) {
                return@get call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "RATE_LIMITED"))
            }
            val results = musicService.getTopTracks()
            call.respond(mapOf("tracks" to results))
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.requireUserOrNull(
    verifier: FirebaseTokenVerifier
) = try {
    requireUser(verifier)
} catch (_: Exception) {
    null
}
