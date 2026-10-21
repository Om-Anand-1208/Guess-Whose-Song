package com.guesswhosesong.server.redis

import com.guesswhosesong.shared.dto.GWSJson
import com.guesswhosesong.shared.models.RoomChatMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** Room chat survives server restarts and is removed with its room. */
class RoomChatRepository(private val redis: RedisClient) {
    companion object {
        private const val TTL_SECONDS = 4L * 60L * 60L
        private const val PAGE_SIZE = 50L
        private fun key(joinCode: String) = "gws:v2:chat:$joinCode"
    }

    /** Newest page by default; [beforeId] loads the next older page without index drift. */
    fun recent(joinCode: String, beforeId: String? = null): List<RoomChatMessage> {
        val chatKey = key(joinCode)
        val newestFirst = if (beforeId == null) {
            redis.lrange(chatKey, 0, PAGE_SIZE - 1)
        } else {
            var start = 0L
            var page: List<String> = emptyList()
            while (true) {
                val chunk = redis.lrange(chatKey, start, start + PAGE_SIZE - 1)
                if (chunk.isEmpty()) break
                val cursorIndex = chunk.indexOfFirst { entry ->
                    runCatching { GWSJson.decodeFromString<RoomChatMessage>(entry).id == beforeId }.getOrDefault(false)
                }
                if (cursorIndex >= 0) {
                    val olderStart = start + cursorIndex + 1
                    page = redis.lrange(chatKey, olderStart, olderStart + PAGE_SIZE - 1)
                    break
                }
                start += chunk.size
            }
            page
        }
        return newestFirst.asReversed().mapNotNull { entry ->
            runCatching { GWSJson.decodeFromString<RoomChatMessage>(entry) }.getOrNull()
        }
    }

    fun append(joinCode: String, message: RoomChatMessage) {
        val key = key(joinCode)
        redis.lpush(key, GWSJson.encodeToString(message))
        redis.expire(key, TTL_SECONDS)
    }

    fun refreshTtl(joinCode: String) = redis.expire(key(joinCode), TTL_SECONDS)

    fun delete(joinCode: String) = redis.del(key(joinCode))
}
