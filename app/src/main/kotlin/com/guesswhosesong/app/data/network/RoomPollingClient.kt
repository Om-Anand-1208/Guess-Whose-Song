package com.guesswhosesong.app.data.network

import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.JoinRoom
import com.guesswhosesong.shared.dto.Kicked
import com.guesswhosesong.shared.dto.RoomEnded
import com.guesswhosesong.shared.dto.ServerMessage
import com.guesswhosesong.shared.dto.toJson
import com.guesswhosesong.shared.models.AvatarCatalog
import com.guesswhosesong.shared.models.AvatarCustomization
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

private const val INITIAL_RETRY_DELAY_MS = 1_000L
private const val MAX_RETRY_DELAY_MS = 15_000L

/** Authenticated room event polling and action requests for the Android client. */
class RoomPollingClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val identityManager: PlayerIdentityManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 128)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

    private var connectionJob: Job? = null
    @Volatile private var intentionallyDisconnected = true
    @Volatile private var currentJoinCode: String? = null

    fun connect(
        joinCode: String,
        displayName: String,
        avatarId: String,
        avatarCustomization: AvatarCustomization? = null
    ) {
        val code = joinCode.uppercase()
        val previousCode = currentJoinCode
        intentionallyDisconnected = false
        connectionJob?.cancel()
        currentJoinCode = code
        _lastError.value = null
        _connectionState.value = ConnectionState.CONNECTING
        connectionJob = scope.launch {
            if (previousCode != null && previousCode != code) leaveRoom(previousCode)
            val normalizedCustomization = AvatarCustomization.normalize(
                avatarCustomization,
                AvatarCatalog.normalize(avatarId)
            )
            var retryDelay = INITIAL_RETRY_DELAY_MS

            while (isActive && !intentionallyDisconnected) {
                try {
                    joinRoom(code, displayName, normalizedCustomization)
                    _connectionState.value = ConnectionState.CONNECTED
                    _lastError.value = null
                    retryDelay = INITIAL_RETRY_DELAY_MS

                    while (isActive && !intentionallyDisconnected) {
                        val response = httpClient.get("$baseUrl/rooms/$code/events") {
                            bearerAuth(identityManager.getIdToken())
                        }
                        if (!response.status.isSuccess()) {
                            throw HttpStatusException(response.status.value, response.bodyAsText())
                        }
                        val events = response.body<List<ServerMessage>>()
                        for (event in events) {
                            _messages.emit(event)
                            if (event is RoomEnded || event is Kicked) {
                                intentionallyDisconnected = true
                                currentJoinCode = null
                                _connectionState.value = ConnectionState.DISCONNECTED
                                return@launch
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    _lastError.value = error.message ?: "Room connection failed"
                    if (error is HttpStatusException && error.status in setOf(401, 403, 409)) {
                        intentionallyDisconnected = true
                        _connectionState.value = ConnectionState.DISCONNECTED
                        return@launch
                    }
                    if (!isActive || intentionallyDisconnected) break
                    _connectionState.value = ConnectionState.RECONNECTING
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                }
            }
        }
    }

    suspend fun send(message: ClientMessage) {
        val code = currentJoinCode ?: return
        val response = httpClient.post("$baseUrl/rooms/$code/actions") {
            bearerAuth(identityManager.getIdToken())
            contentType(ContentType.Application.Json)
            setBody(message.toJson())
        }
        if (!response.status.isSuccess()) {
            throw HttpStatusException(response.status.value, response.bodyAsText())
        }
    }

    fun disconnect() {
        val code = currentJoinCode
        intentionallyDisconnected = true
        currentJoinCode = null
        connectionJob?.cancel()
        connectionJob = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _lastError.value = null
        if (code != null) scope.launch { leaveRoom(code) }
    }

    private suspend fun joinRoom(code: String, displayName: String, customization: AvatarCustomization) {
        val response = httpClient.post("$baseUrl/rooms/$code/join") {
            bearerAuth(identityManager.getIdToken())
            contentType(ContentType.Application.Json)
            setBody(
                JoinRoom(
                    displayName = displayName,
                    avatarId = customization.shapeId,
                    avatarCustomization = customization
                ).toJson()
            )
        }
        if (!response.status.isSuccess()) {
            throw HttpStatusException(response.status.value, response.bodyAsText())
        }
    }

    private suspend fun leaveRoom(code: String) {
        runCatching {
            httpClient.delete("$baseUrl/rooms/$code/join") {
                bearerAuth(identityManager.getIdToken())
            }
        }
    }

    private class HttpStatusException(val status: Int, details: String) :
        IllegalStateException("Room request failed ($status)${details.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}")
}
