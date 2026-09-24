package com.guesswhosesong.server

import com.guesswhosesong.server.engine.RoomManager
import com.guesswhosesong.server.plugins.*
import com.guesswhosesong.server.redis.RedisClient
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
    val roomManager = RoomManager(redisClient)

    // Install Ktor plugins
    configureSerialization()
    configureCors()
    configureWebSockets()
    configureStatusPages()
    configureCallLogging()

    // Mount routes
    configureRouting(roomManager, redisClient)
}
