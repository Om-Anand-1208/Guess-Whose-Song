package com.guesswhosesong.server

import com.guesswhosesong.server.auth.FirebaseTokenVerifier
import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.plugins.*
import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.routes.closeSpotifyResources
import com.guesswhosesong.server.spotify.SpotifyClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toIntOrNull() ?: 8080) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Initialize singletons
    val redisClient = RedisClient.fromEnv()
    val spotifyClient = SpotifyClient()
    val roomManager = RoomManager(redisClient, spotifyClient = spotifyClient)
    val tokenVerifier = FirebaseTokenVerifier.fromEnvironment()

    // Install Ktor plugins
    configureSerialization()
    configureCors()
    configureStatusPages()
    configureCallLogging()

    // Mount routes
    configureRouting(roomManager, redisClient, tokenVerifier, spotifyClient)

    environment.monitor.subscribe(ApplicationStopping) {
        roomManager.musicService.close()
        spotifyClient.close()
        closeSpotifyResources()
        redisClient.close()
    }
}
