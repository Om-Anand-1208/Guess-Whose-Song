package com.guesswhosesong.app.data.network

import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.shared.dto.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.header
import io.ktor.websocket.*
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private const val RECONNECT_DELAY_MS = 2_000L
private const val MAX_RECONNECT_DELAY_MS = 30_000L

/**
 * Manages a single WebSocket connection to the backend.
 * Exposes:
 *  - [messages] — a SharedFlow of [ServerMessage] for the UI to collect.
 *  - [send] — to send [ClientMessage]s.
 *  - [connect] / [disconnect] — lifecycle management.
 */
class WebSocketManager(
    private val httpClient: HttpClient,
    private val wsBaseUrl: String,
    private val identityManager: PlayerIdentityManager
) {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ServerMessage> = _messages.asSharedFlow()

    private val outgoingMessages = Channel<String>(Channel.BUFFERED)

    private var connectionScope: CoroutineScope? = null
    private var currentJoinCode: String? = null
    private var currentDisplayName: String? = null
    @Volatile private var stopReconnect = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING }

    /**
     * Connect to a room WebSocket.
     * Automatically reconnects on drop with exponential backoff.
     */
    fun connect(joinCode: String, displayName: String) {
        stopReconnect = true
        connectionScope?.cancel()
        drainOutgoing()
        stopReconnect = false
        currentJoinCode = joinCode
        currentDisplayName = displayName

        connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        connectionScope!!.launch {
            var delay = RECONNECT_DELAY_MS
            while (isActive) {
                try {
                    _connectionState.value = ConnectionState.CONNECTING
                    val token = identityManager.getIdToken()

                    httpClient.webSocket(
                        urlString = "$wsBaseUrl/rooms/${joinCode.uppercase()}/ws",
                        request = { header(HttpHeaders.Authorization, "Bearer $token") }
                    ) {
                        _connectionState.value = ConnectionState.CONNECTED
                        _lastError.value = null
                        delay = RECONNECT_DELAY_MS // reset backoff on success
                        send(Frame.Text(JoinRoom(displayName).toJson()))

                        // Fan out: send queued outgoing messages
                        val sendJob = launch {
                            for (msg in outgoingMessages) {
                                try { send(Frame.Text(msg)) }
                                catch (_: Exception) { break }
                            }
                        }

                        // Receive incoming messages
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                try {
                                    val msg = frame.readText().toServerMessage()
                                    _messages.emit(msg)
                                    if (msg is RoomEnded) stopReconnect = true
                                } catch (e: Exception) {
                                    logger.warn("Failed to parse server message: ${e.message}")
                                }
                            }
                        }
                        sendJob.cancel()
                        val reason = closeReason.await()
                        if (reason?.message in setOf("AUTH_REQUIRED", "JOIN_REQUIRED", "RATE_LIMITED", "room ended")) {
                            stopReconnect = true
                        }
                    }
                } catch (e: Exception) {
                    val errorString = e.stackTraceToString().take(500)
                    logger.warn("WebSocket error: $errorString")
                    _lastError.value = "WS Error: ${e.toString().take(100)}"
                    if (e.message.orEmpty().contains("AUTH_REQUIRED", ignoreCase = true) ||
                        e.message.orEmpty().contains("401")) {
                        stopReconnect = true
                    }
                }

                if (!isActive || stopReconnect) break
                _connectionState.value = ConnectionState.RECONNECTING
                delay(delay.milliseconds)
                delay = (delay * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
            }
        }
    }

    /**
     * Send a [ClientMessage] to the server.
     * Messages are buffered if the socket is temporarily unavailable.
     */
    suspend fun send(message: ClientMessage) {
        outgoingMessages.send(message.toJson())
    }

    fun disconnect() {
        stopReconnect = true
        connectionScope?.cancel()
        connectionScope = null
        drainOutgoing()
        currentJoinCode = null
        currentDisplayName = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _lastError.value = null
    }

    private fun drainOutgoing() {
        while (outgoingMessages.tryReceive().isSuccess) { /* discard stale room messages */ }
    }
}
