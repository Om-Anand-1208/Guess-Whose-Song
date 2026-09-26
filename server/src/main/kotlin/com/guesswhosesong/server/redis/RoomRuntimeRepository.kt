package com.guesswhosesong.server.redis

import kotlinx.serialization.Serializable
import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/** Private state-machine metadata; it is never sent as part of a public Room snapshot. */
@Serializable
data class RoomRuntime(
    val phase: String = "LOBBY",
    val roundIndex: Int = 0,
    val deadlineEpochMillis: Long = 0L,
    val runId: String = "",
    val votes: Map<String, String> = emptyMap(),
    val roundResolved: Boolean = false
)

class RoomRuntimeRepository(private val redis: RedisClient) {
    companion object {
        private const val TTL_SECONDS = 4L * 60L * 60L
        fun key(joinCode: String) = "gws:v2:runtime:$joinCode"
    }

    fun save(joinCode: String, runtime: RoomRuntime) {
        redis.set(key(joinCode), GWSJson.encodeToString(runtime), TTL_SECONDS)
    }

    fun load(joinCode: String): RoomRuntime? = redis.get(key(joinCode))?.let {
        runCatching { GWSJson.decodeFromString<RoomRuntime>(it) }.getOrNull()
    }

    fun delete(joinCode: String) = redis.del(key(joinCode))
}
