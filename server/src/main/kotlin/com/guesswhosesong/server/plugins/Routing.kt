package com.guesswhosesong.server.plugins

import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.routes.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(roomManager: RoomManager, redisClient: RedisClient) {
    routing {
        // Health check
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // Room REST + WebSocket
        roomRoutes(roomManager)

        // Music proxy (Deezer + iTunes fallback)
        musicRoutes()

        // Spotify OAuth + data routes (optional — gracefully unavailable if not configured)
        spotifyRoutes(redisClient)
    }
}
