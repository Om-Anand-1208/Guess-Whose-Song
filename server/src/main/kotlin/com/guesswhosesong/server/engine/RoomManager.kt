package com.guesswhosesong.server.engine

import com.guesswhosesong.server.redis.RedisClient
import com.guesswhosesong.server.redis.RoomRepository
import com.guesswhosesong.server.music.MusicService
import com.guesswhosesong.shared.models.*
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Global registry of active [RoomSession]s.
 * Acts as the in-memory hot-path; Redis is the durable backup.
 */
class RoomManager(
    val redisClient: RedisClient,
    val musicService: MusicService = MusicService()
) {

    private val logger = LoggerFactory.getLogger(RoomManager::class.java)
    private val sessions = ConcurrentHashMap<String, RoomSession>()
    private val repository = RoomRepository(redisClient)
    private val random = SecureRandom()

    /**
     * Create a new room. Returns the created [RoomSession].
     */
    fun createRoom(hostId: String, hostName: String): RoomSession {
        val joinCode = generateJoinCode()
        val room = Room(
            id = joinCode,
            joinCode = joinCode,
            hostId = hostId,
            state = RoomState.LOBBY,
            createdAt = System.currentTimeMillis(),
            players = listOf(
                Player(
                    id = hostId,
                    displayName = hostName,
                    isHost = true,
                    joinedAt = System.currentTimeMillis()
                )
            )
        )
        val session = RoomSession(room, redisClient, ::deleteRoom, musicService)
        sessions[joinCode] = session
        repository.save(room)
        logger.info("Room created: $joinCode")
        return session
    }

    /**
     * Find an existing session. If not in memory, attempt to rehydrate from Redis.
     */
    fun findRoom(joinCode: String): RoomSession? {
        return sessions[joinCode] ?: rehydrateFromRedis(joinCode)
    }

    fun deleteRoom(joinCode: String) {
        sessions.remove(joinCode)
        repository.delete(joinCode)
        logger.info("Room deleted: $joinCode")
    }

    fun roomExists(joinCode: String): Boolean {
        return sessions.containsKey(joinCode) || repository.exists(joinCode)
    }

    private fun rehydrateFromRedis(joinCode: String): RoomSession? {
        val room = repository.load(joinCode) ?: return null
        val session = RoomSession(room, redisClient, ::deleteRoom, musicService)
        val active = sessions.putIfAbsent(joinCode, session) ?: session
        if (active === session) active.resumeAfterRestart()
        logger.info("Rehydrated room from Redis: $joinCode")
        return active
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no ambiguous chars (0/O, 1/I)
        var code: String
        do {
            code = (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
        } while (sessions.containsKey(code) || repository.exists(code))
        return code
    }
}
