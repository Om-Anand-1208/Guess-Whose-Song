package com.guesswhosesong.server.redis

import com.guesswhosesong.shared.models.Room
import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Persistence layer for Room objects.
 * Rooms are stored as JSON blobs under key "room:{joinCode}".
 * TTL = 4 hours (safety-net expiry; explicit delete fires on ENDED state).
 */
class RoomRepository(private val redis: RedisClient) {

    companion object {
        private const val ROOM_TTL_SECONDS = 4L * 60L * 60L // 4 hours
        // v2 is an intentional hard-cutover namespace; old self-asserted rooms cannot re-enter.
        fun roomKey(joinCode: String) = "gws:v2:room:$joinCode"
    }

    fun save(room: Room) {
        val json = GWSJson.encodeToString(room)
        redis.set(roomKey(room.joinCode), json, ROOM_TTL_SECONDS)
    }

    fun load(joinCode: String): Room? {
        val json = redis.get(roomKey(joinCode)) ?: return null
        return try {
            GWSJson.decodeFromString<Room>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun delete(joinCode: String) {
        redis.del(roomKey(joinCode))
    }

    fun exists(joinCode: String): Boolean = redis.exists(roomKey(joinCode))

    /** Refresh TTL without a full re-save (for example, on room activity). */
    fun refreshTtl(joinCode: String) {
        redis.expire(roomKey(joinCode), ROOM_TTL_SECONDS)
    }
}
