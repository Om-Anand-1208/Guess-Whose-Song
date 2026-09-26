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
        val configuredOrigins = System.getenv("WEB_ORIGINS")
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
        configuredOrigins.forEach { origin ->
            val uri = java.net.URI(origin)
            allowHost(
                host = uri.host ?: return@forEach,
                schemes = listOf(uri.scheme ?: "https")
            )
        }
    }
}
