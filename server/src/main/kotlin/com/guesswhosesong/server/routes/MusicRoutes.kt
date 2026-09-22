package com.guesswhosesong.server.routes

import com.guesswhosesong.server.itunes.ItunesClient
import com.guesswhosesong.server.music.MusicService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private val itunesClient = ItunesClient()
private val musicService = MusicService()

/**
 * Proxy route for iTunes search and discovery.
 * Clients call this instead of iTunes directly (avoids CORS, encapsulates API).
 * Proxy route for unified music search (Deezer with iTunes fallback).
 * Clients call this instead of music APIs directly (avoids CORS, encapsulates API).
 *
 * GET /itunes/search?q=<query>&limit=<n>
 * GET /itunes/top
 * GET /music/search?q=<query>&limit=<n>
 * GET /music/top
 */
fun Route.itunesRoutes() {
    route("/itunes") {
fun Route.musicRoutes() {
    route("/music") {
        get("/search") {
            val query = call.request.queryParameters["q"]?.trim()
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing query parameter 'q'"))
                return@get
            }
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 50) ?: 10
            val results = itunesClient.search(query, limit)
            val results = musicService.search(query, limit)
            call.respond(mapOf("tracks" to results))
        }

        get("/top") {
            // As a fallback for top genres/playlists, we use an empty query or top artists
            // Apple Music top charts typically require RSS parsing. For v1, we can return
            // a hardcoded top list or just do a general search for "Hits" as a placeholder.
            val results = itunesClient.search("Hits 2024", 20)
            val results = musicService.getTopTracks()
            call.respond(mapOf("tracks" to results))
        }
    }
}

