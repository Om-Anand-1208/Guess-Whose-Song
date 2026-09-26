package com.guesswhosesong.server.redis

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import com.guesswhosesong.shared.dto.GWSJson
import java.security.SecureRandom
import java.util.Base64

/** A short-lived, single-use credential for browser WebSocket handshakes. */
@Serializable
data class WsTicket(
    val uid: String,
    val joinCode: String,
    val expiresAtEpochMillis: Long
)

class WsTicketRepository(private val redis: RedisClient) {
    companion object {
        private const val TTL_SECONDS = 30L
        private const val PREFIX = "gws:v2:ws-ticket:"
        private val random = SecureRandom()

        fun key(ticket: String): String = PREFIX + ticket

        private fun randomTicket(): String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also(random::nextBytes))
    }

    fun create(uid: String, joinCode: String): String {
        val ticket = randomTicket()
        val expiresAt = System.currentTimeMillis() + TTL_SECONDS * 1_000L
        val value = GWSJson.encodeToString(WsTicket(uid, joinCode, expiresAt))
        check(redis.setIfAbsent(key(ticket), value, TTL_SECONDS)) {
            "Could not reserve a WebSocket ticket"
        }
        return ticket
    }

    fun consume(ticket: String): WsTicket? {
        if (ticket.length !in 40..128 || !ticket.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            return null
        }
        val raw = redis.getAndDelete(key(ticket)) ?: return null
        val parsed = runCatching { GWSJson.decodeFromString<WsTicket>(raw) }.getOrNull() ?: return null
        return parsed.takeIf { it.expiresAtEpochMillis >= System.currentTimeMillis() }
    }
}
