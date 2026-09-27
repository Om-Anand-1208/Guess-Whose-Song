@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.ClientMessage
import com.guesswhosesong.shared.dto.JoinRoom
import com.guesswhosesong.shared.dto.RoomEnded
import com.guesswhosesong.shared.dto.ServerMessage
import com.guesswhosesong.shared.dto.toJson
import com.guesswhosesong.shared.dto.toServerMessage
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.js.JsAny

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED }

class WebSocketGameClient(private val api: WebApiClient) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sendMutex = Mutex()
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    val messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 128)

    private var socket: JsAny? = null
    private var roomCode: String? = null
    private var displayName: String? = null
    private var avatarId: String? = null
    private var reconnectJob: Job? = null
    private var intentionallyDisconnected = true
    private var stopReconnect = false
    private val outgoing = ArrayDeque<String>()

    suspend fun connect(joinCode: String, name: String, selectedAvatarId: String) {
        disconnect()
        roomCode = joinCode.uppercase()
        displayName = name
        avatarId = selectedAvatarId
        intentionallyDisconnected = false
        stopReconnect = false
        _lastError.value = null
        _state.value = ConnectionState.CONNECTING
        try {
            connectOnce()
        } catch (error: Throwable) {
            _state.value = ConnectionState.FAILED
            _lastError.value = error.message ?: "WebSocket connection failed"
            intentionallyDisconnected = true
            stopReconnect = true
            throw error
        }
    }

    private suspend fun connectOnce() {
        val code = roomCode ?: return
        val name = displayName ?: return
        val selectedAvatarId = avatarId ?: return
        val ticket = api.webSocketTicket(code)
        val opened = CompletableDeferred<Unit>()
        val url = "${configuredWsBaseUrl().trimEnd('/')}/rooms/$code/ws"
        socket = browserOpenSocket(
            url = url,
            protocol = "gws-ticket",
            ticketProtocol = "gws-ticket.$ticket",
            onOpen = {
                _state.value = ConnectionState.CONNECTED
                sendImmediately(JoinRoom(name, selectedAvatarId))
                opened.complete(Unit)
            },
            onMessage = { text ->
                runCatching { text.toServerMessage() }.onSuccess { message ->
                    messages.tryEmit(message)
                    if (message is RoomEnded) stopReconnect = true
                }
            },
            onClose = { _, reason ->
                socket = null
                if (!intentionallyDisconnected) {
                    _lastError.value = reason.ifBlank {
                        if (opened.isCompleted) "WebSocket connection closed" else "WebSocket closed before joining"
                    }
                }
                if (reason == "AUTH_REQUIRED") {
                    stopReconnect = true
                    _state.value = ConnectionState.FAILED
                }
                if (!opened.isCompleted) opened.completeExceptionally(IllegalStateException("WebSocket closed before joining"))
                if (!intentionallyDisconnected && !stopReconnect) scheduleReconnect()
                else if (_state.value != ConnectionState.FAILED) _state.value = ConnectionState.DISCONNECTED
            },
            onError = {
                _lastError.value = "WebSocket connection failed"
                if (!opened.isCompleted) opened.completeExceptionally(IllegalStateException("WebSocket connection failed"))
            }
        )
        opened.await()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            repeat(5) { attempt ->
                if (intentionallyDisconnected || stopReconnect) return@launch
                _state.value = ConnectionState.RECONNECTING
                delay((1_000L shl attempt).coerceAtMost(15_000L))
                try {
                    connectOnce()
                    return@launch
                } catch (error: Throwable) {
                    if (error is WebApiException && error.status == 401) {
                        stopReconnect = true
                        intentionallyDisconnected = true
                        _state.value = ConnectionState.FAILED
                        return@launch
                    }
                }
            }
            _state.value = ConnectionState.FAILED
        }
    }

    suspend fun send(message: ClientMessage) {
        sendMutex.withLock {
            val encoded = message.toJson()
            val active = socket
            if (active == null || _state.value != ConnectionState.CONNECTED) {
                if (outgoing.size >= 64) outgoing.removeFirst()
                outgoing.addLast(encoded)
            } else {
                browserSendSocket(active, encoded)
            }
        }
    }

    private fun sendImmediately(message: ClientMessage) {
        scope.launch {
            sendMutex.withLock {
                val active = socket ?: return@withLock
                browserSendSocket(active, message.toJson())
                while (outgoing.isNotEmpty()) browserSendSocket(active, outgoing.removeFirst())
            }
        }
    }

    fun disconnect() {
        intentionallyDisconnected = true
        stopReconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        outgoing.clear()
        socket?.let { runCatching { browserCloseSocket(it, 1000, "client disconnect") } }
        socket = null
        roomCode = null
        displayName = null
        _state.value = ConnectionState.DISCONNECTED
    }

    fun close() {
        disconnect()
        scope.coroutineContext[Job]?.cancel()
    }
}
