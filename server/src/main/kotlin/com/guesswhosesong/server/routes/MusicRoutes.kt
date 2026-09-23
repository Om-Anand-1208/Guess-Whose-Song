package com.guesswhosesong.server.routes

import com.guesswhosesong.server.music.MusicService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val musicService = MusicService()

/**
 * Proxy route for unified music search (Deezer with iTunes fallback).
 * Clients call this instead of music APIs directly (avoids CORS, encapsulates API).
 *
 * GET /music/search?q=<query>&limit=<n>
 * GET /music/top
 */
fun Route.musicRoutes() {
    route("/music") {
        get("/search") {
            val query = call.request.queryParameters["q"]?.trim()
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing query parameter 'q'"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
            val results = musicService.search(query, limit)
            call.respond(mapOf("tracks" to results))
        }

        get("/top") {
            val results = musicService.getTopTracks()
            call.respond(mapOf("tracks" to results))
        }
    }
}
