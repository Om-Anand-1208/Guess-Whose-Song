package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.shared.models.AvatarCustomization
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomResponse(
    val joinCode: String
)

@Serializable
data class CreateRoomRequest(
    val displayName: String,
    val avatarId: String,
    val avatarCustomization: AvatarCustomization
)

@Serializable
data class RoomInfoResponse(
    val joinCode: String,
    val state: String,
    val playerCount: Int,
    val playerLimit: Int
)

/**
 * HTTP repository for room management REST calls.
 */
class RoomRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val identityManager: PlayerIdentityManager
) {

    private val serverWakeMutex = Mutex()
    private val serverWakeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverWakeAttempt: Deferred<Boolean>? = null
    @Volatile
    private var lastHealthyAtMillis: Long = 0L

    /**
     * Wakes a sleeping free-tier server and waits until it is ready.
     * Concurrent callers share the same check, and a recent success avoids
     * adding a health request before every room action.
     */
    suspend fun awaitServerReady(maxWaitMillis: Long = 90_000L): Boolean {
        if (hasRecentHealthCheck()) return true

        val attempt = serverWakeMutex.withLock {
            serverWakeAttempt?.takeIf { it.isActive }
                ?: serverWakeScope.async { performServerWake(maxWaitMillis) }
                    .also { serverWakeAttempt = it }
        }
        return attempt.await()
    }

    private suspend fun performServerWake(maxWaitMillis: Long): Boolean =
        withTimeoutOrNull(maxWaitMillis) {
            while (true) {
                val healthy = try {
                    httpClient.get("$baseUrl/health") {
                        timeout {
                            connectTimeoutMillis = 10_000L
                            requestTimeoutMillis = 20_000L
                            socketTimeoutMillis = 20_000L
                        }
                    }.status.isSuccess()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    false
                }

                if (healthy) {
                    lastHealthyAtMillis = System.currentTimeMillis()
                    break
                }
                delay(1_500L)
            }
            true
        } ?: false

    private fun hasRecentHealthCheck(): Boolean =
        System.currentTimeMillis() - lastHealthyAtMillis < HEALTH_CHECK_FRESHNESS_MILLIS

    /**
     * Creates a new room. Returns the join code.
     */
    suspend fun createRoom(
        displayName: String,
        avatarId: String,
        avatarCustomization: AvatarCustomization = AvatarCustomization.defaultsFor(avatarId)
    ): Result<CreateRoomResponse> {
        return runCatching {
            val idToken = identityManager.getIdToken()
            httpClient.post("$baseUrl/rooms") {
                contentType(ContentType.Application.Json)
                bearerAuth(idToken)
                setBody(
                    CreateRoomRequest(
                        displayName = displayName,
                        avatarId = avatarCustomization.shapeId,
                        avatarCustomization = avatarCustomization
                    )
                )
            }.body<CreateRoomResponse>()
        }
    }

    /**
     * Fetches basic room info for lobby preview.
     */
    suspend fun getRoomInfo(joinCode: String): Result<RoomInfoResponse> {
        return runCatching {
            httpClient.get("$baseUrl/rooms/${joinCode.uppercase()}") {
                bearerAuth(identityManager.getIdToken())
            }.body<RoomInfoResponse>()
        }
    }

    private companion object {
        // Render sleeps a free service after 15 idle minutes. Rechecking a few
        // minutes earlier avoids trusting a server that may have just slept.
        const val HEALTH_CHECK_FRESHNESS_MILLIS = 12 * 60 * 1_000L
    }
}
