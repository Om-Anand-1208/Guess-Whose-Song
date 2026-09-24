package com.guesswhosesong.app.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.guesswhosesong.app.di.NetworkModule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the optional Spotify OAuth flow.
 * Opens a Chrome Custom Tab for the Spotify authorization page hosted on our server.
 * The server handles the code exchange; on success, the Android app receives a deep link:
 * guesswhosesong://spotify-callback?success=true
 */
@Singleton
class SpotifyAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gws_spotify", Context.MODE_PRIVATE)

    private val _isConnected = MutableStateFlow(prefs.getBoolean("is_connected", false))
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Opens Spotify OAuth in a Chrome Custom Tab (or browser fallback).
     * Pass the playerId as the state param so the server can link the token to the player.
     */
    fun launchOAuth(playerId: String) {
        val authUrl = "${NetworkModule.BASE_URL}/spotify/auth?state=$playerId"
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            customTabsIntent.launchUrl(context, Uri.parse(authUrl))
        } catch (_: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        }
    }

    /**
     * Called when the deep-link callback arrives (from MainActivity or a NavController handler).
     */
    fun onCallbackReceived(success: Boolean) {
        prefs.edit().putBoolean("is_connected", success).apply()
        _isConnected.value = success
    }

    fun disconnect() {
        prefs.edit().putBoolean("is_connected", false).apply()
        _isConnected.value = false
    }
}
