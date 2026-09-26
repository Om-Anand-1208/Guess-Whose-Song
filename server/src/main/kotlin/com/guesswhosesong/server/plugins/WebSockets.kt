package com.guesswhosesong.server.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureWebSockets() {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = 256 * 1024L
        masking = false
    }
}
