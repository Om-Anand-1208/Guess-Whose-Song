package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.Kicked
import com.guesswhosesong.shared.dto.RoomEnded
import com.guesswhosesong.shared.dto.ServerMessage
import com.guesswhosesong.shared.models.AvatarCustomization
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val INITIAL_RETRY_DELAY_MS = 1_000L
private const val MAX_RETRY_DELAY_MS = 15_000L

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED }

/** Authenticated room event polling and action requests for the browser client. */
class RoomPollingClient(private val api: WebApiClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    val messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 128)

    private var connectionJob: Job? = null
    private var roomCode: String? = null
    private var displayName: String? = null
    private var avatarId: String? = null
    private var avatarCustomization: AvatarCustomization? = null
    private var intentionallyDisconnected = true

    fun connect(
        joinCode: String,
        name: String,
        selectedAvatarId: String,
        selectedAvatarCustomization: AvatarCustomization
    ) {
        val code = joinCode.uppercase()
        val previousCode = roomCode
        connectionJob?.cancel()
        roomCode = code
        displayName = name
        avatarId = selectedAvatarId
        avatarCustomization = selectedAvatarCustomization
        intentionallyDisconnected = false
        _lastError.value = null
        _state.value = ConnectionState.CONNECTING

        connectionJob = scope.launch {
            if (previousCode != null && previousCode != code) runCatching { api.leaveRoom(previousCode) }
            var retryDelay = INITIAL_RETRY_DELAY_MS
            while (true) {
                try {
                    api.joinRoom(
                        code,
                        displayName ?: return@launch,
                        avatarId ?: return@launch,
                        avatarCustomization ?: return@launch
                    )
                    _state.value = ConnectionState.CONNECTED
                    _lastError.value = null
                    retryDelay = INITIAL_RETRY_DELAY_MS

                    while (!intentionallyDisconnected) {
                        for (event in api.pollRoomEvents(code)) {
                            messages.emit(event)
                            if (event is Kicked || event is RoomEnded) {
                                intentionallyDisconnected = true
                                roomCode = null
                                _state.value = ConnectionState.DISCONNECTED
                                return@launch
                            }
                        }
                    }
                    return@launch
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    _lastError.value = error.message ?: "Room connection failed"
                    if (error is WebApiException && error.status in setOf(401, 403, 409)) {
                        intentionallyDisconnected = true
                        _state.value = ConnectionState.FAILED
                        return@launch
                    }
                    if (intentionallyDisconnected) return@launch
                    _state.value = ConnectionState.RECONNECTING
                    delay(retryDelay)
                    retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
                }
            }
        }
    }

    suspend fun send(message: ClientMessage) {
        val code = roomCode ?: return
        api.sendRoomAction(code, message)
    }

    fun disconnect() {
        val previousCode = roomCode
        intentionallyDisconnected = true
        connectionJob?.cancel()
        connectionJob = null
        roomCode = null
        displayName = null
        avatarId = null
        avatarCustomization = null
        _state.value = ConnectionState.DISCONNECTED
        if (previousCode != null) scope.launch { runCatching { api.leaveRoom(previousCode) } }
    }

    fun close() {
        disconnect()
        scope.coroutineContext[Job]?.cancel()
    }
}
