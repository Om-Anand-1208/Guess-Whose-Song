package com.guesswhosesong.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCors() {
    install(CORS) {
        // The browser API uses Firebase bearer tokens. Keep credentials
        // explicitly enabled while restricting requests to configured origins.
        allowCredentials = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("ngrok-skip-browser-warning")
        // Firebase Hosting defaults keep the deployed web client working even
        // when WEB_ORIGINS has not been added to a local server environment.
        // Local origins are useful for running the Wasm client during development.
        val defaultOrigins = listOf(
            "https://guess-whose-song.web.app",
            "https://guess-whose-song.firebaseapp.com",
            "http://localhost:8080",
            "http://localhost:8081",
            "http://127.0.0.1:8080",
            "http://127.0.0.1:8081"
        )
        val configuredOrigins = defaultOrigins + System.getenv("WEB_ORIGINS")
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
        configuredOrigins.distinct().forEach { origin ->
            runCatching {
                val uri = java.net.URI(origin)
                val host = uri.host ?: return@runCatching
                val scheme = uri.scheme?.lowercase() ?: return@runCatching
                if (scheme in setOf("http", "https")) {
                    allowHost(host = host, schemes = listOf(scheme))
                }
            }
        }
    }
}
