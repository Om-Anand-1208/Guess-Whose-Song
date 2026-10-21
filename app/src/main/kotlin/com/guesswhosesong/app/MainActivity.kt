package com.guesswhosesong.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.guesswhosesong.app.data.repository.GameRepository
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.app.ui.GWSNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingInviteCode = mutableStateOf<String?>(null)

    @Inject
    lateinit var spotifyAuthManager: SpotifyAuthManager

    @Inject
    lateinit var gameRepository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        enableEdgeToEdge()
        setContent {
            GWSNavHost(
                pendingInviteCode = pendingInviteCode.value,
                onInviteHandled = { pendingInviteCode.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        if (isFinishing) gameRepository.disconnect()
        super.onDestroy()
    }

    private fun handleSpotifyIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "guesswhosesong" && uri.host == "spotify-callback") {
            val success = uri.getQueryParameter("success") == "true"
            spotifyAuthManager.onCallbackReceived(success)
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        handleSpotifyIntent(intent)
        if (intent?.action == Intent.ACTION_VIEW) {
            RoomInviteLinks.codeFrom(intent.data)?.let { pendingInviteCode.value = it }
        }
    }
}
