package com.guesswhosesong.app.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.guesswhosesong.app.di.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the optional Spotify OAuth flow.
 * Opens a Chrome Custom Tab for the Spotify authorization page hosted on our server.
 * The server handles the code exchange; on success, the Android app receives a deep link:
 * guesswhosesong://spotify-callback?success=true
 */
class SpotifyAuthManager(private val context: Context) {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Opens Spotify OAuth in a Chrome Custom Tab.
     * Pass the Firebase UID as the state param so the server can link the token to the player.
     */
    fun launchOAuth(firebaseUid: String) {
        val authUrl = "${NetworkModule.BASE_URL}/spotify/auth?state=$firebaseUid"
        CustomTabsIntent.Builder()
            .build()
            .launchUrl(context, Uri.parse(authUrl))
    }

    /**
     * Called when the deep-link callback arrives (from MainActivity or a NavController handler).
     */
    fun onCallbackReceived(success: Boolean) {
        _isConnected.value = success
    }
}
