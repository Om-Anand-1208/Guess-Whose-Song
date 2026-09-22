package com.guesswhosesong.server.redis

import com.guesswhosesong.shared.models.ChatMessage
import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Ephemeral chat storage.
 * Messages are stored as a Redis list under "chat:{joinCode}".
 * The list TTL is set to match the room TTL — Redis auto-deletes both together.
 * No durable log is kept; this is intentional per spec.
 */
class ChatRepository(private val redis: RedisClient) {

    companion object {
        private const val MAX_MESSAGES = 200L // keep last 200 in memory for reconnect catch-up
        private const val CHAT_TTL_SECONDS = 4L * 60L * 60L // matches room TTL
        fun chatKey(joinCode: String) = "chat:$joinCode"
    }

    /**
     * Append a new chat message and trim the list to [MAX_MESSAGES].
     * Returns the stored message (with timestamp filled in if missing).
     */
    fun append(joinCode: String, message: ChatMessage): ChatMessage {
        val msg = if (message.timestamp == 0L) message.copy(timestamp = System.currentTimeMillis())
                  else message
        val json = GWSJson.encodeToString(msg)
        val key = chatKey(joinCode)
        redis.lpush(key, json)
        redis.ltrim(key, 0, MAX_MESSAGES - 1)
        redis.expire(key, CHAT_TTL_SECONDS)
        return msg
    }

    /**
     * Retrieve recent messages for a reconnecting client.
     * Returns them oldest-first (list is newest-first in Redis, so we reverse).
     */
    fun getRecent(joinCode: String, limit: Int = 50): List<ChatMessage> {
        return redis.lrange(chatKey(joinCode), 0, limit.toLong() - 1)
            .mapNotNull { json ->
                try { GWSJson.decodeFromString<ChatMessage>(json) } catch (e: Exception) { null }
            }
            .reversed()
    }

    fun delete(joinCode: String) {
        redis.del(chatKey(joinCode))
    }
}

