package com.guesswhosesong.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.guesswhosesong.app.data.spotify.SpotifyAuthManager
import com.guesswhosesong.app.ui.GWSNavHost
import com.guesswhosesong.app.ui.theme.GuessWhoseSongTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var spotifyAuthManager: SpotifyAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleSpotifyIntent(intent)
        enableEdgeToEdge()
        setContent {
            GuessWhoseSongTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GWSNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyIntent(intent)
    }

    private fun handleSpotifyIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "guesswhosesong" && uri.host == "spotify-callback") {
            val success = uri.getQueryParameter("success") == "true"
            spotifyAuthManager.onCallbackReceived(success)
        }
    }
}
