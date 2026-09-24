package com.guesswhosesong.app.data.repository

import com.guesswhosesong.app.data.network.WebSocketManager
import com.guesswhosesong.shared.dto.*
import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import kotlinx.coroutines.flow.SharedFlow

/**
 * Single facade that the ViewModels use for all game interactions.
 * Delegates to [WebSocketManager] for realtime operations.
 */
class GameRepository(private val wsManager: WebSocketManager) {

    val messages: SharedFlow<ServerMessage> = wsManager.messages
    val connectionState = wsManager.connectionState
    val lastError = wsManager.lastError

    fun connect(joinCode: String, token: String, displayName: String) {
        wsManager.connect(joinCode, token, displayName)
    }

    fun disconnect() = wsManager.disconnect()

    suspend fun startGame() = wsManager.send(StartGame())
    suspend fun submitSong(song: SongEntry) = wsManager.send(SubmitSong(song = song))
    suspend fun updatePendingSong(song: SongEntry) = wsManager.send(UpdatePendingSong(song = song))
    suspend fun updatePendingSongs(songs: List<SongEntry>) = wsManager.send(UpdatePendingSongs(songs = songs))
    suspend fun lockSong() = wsManager.send(LockSong())
    suspend fun castVote(guessedPlayerId: String) = wsManager.send(CastVote(guessedPlayerId = guessedPlayerId))
    suspend fun sendChat(text: String) = wsManager.send(SendChat(text = text))
    suspend fun updateSettings(settings: RoomSettings) = wsManager.send(UpdateSettings(settings = settings))
    suspend fun kickPlayer(targetId: String) = wsManager.send(KickPlayer(targetPlayerId = targetId))
    suspend fun playAgain() = wsManager.send(PlayAgain())
    suspend fun endRoom() = wsManager.send(EndRoom())
    suspend fun connectSpotify(accessToken: String) = wsManager.send(ConnectSpotify(accessToken = accessToken))
}
