package com.guesswhosesong.app.data.spotify

import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.di.NetworkModule
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.delete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class SpotifyAuthUrlResponse(val authorizationUrl: String)

@Serializable
private data class SpotifyStatusResponse(val connected: Boolean)

@Singleton
class SpotifyAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    private val identityManager: PlayerIdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init { refreshStatus() }

    fun launchOAuth() {
        scope.launch {
            runCatching {
                val token = identityManager.getIdToken()
                val response = httpClient.get("${NetworkModule.BASE_URL}/spotify/auth-url") {
                    bearerAuth(token)
                }.body<SpotifyAuthUrlResponse>()
                val uri = response.authorizationUrl.toUri()
                try {
                    CustomTabsIntent.Builder().build().launchUrl(context, uri)
                } catch (_: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
    }

    /** The callback only wakes a status refresh; its success query is not trusted. */
    fun onCallbackReceived(@Suppress("UNUSED_PARAMETER") success: Boolean) {
        refreshStatus()
    }

    fun refreshStatus() {
        scope.launch {
            _isConnected.value = runCatching {
                httpClient.get("${NetworkModule.BASE_URL}/spotify/status") {
                    bearerAuth(identityManager.getIdToken())
                }.body<SpotifyStatusResponse>().connected
            }.getOrDefault(false)
        }
    }

    fun disconnect() {
        scope.launch {
            runCatching {
                httpClient.delete("${NetworkModule.BASE_URL}/spotify/connection") {
                    bearerAuth(identityManager.getIdToken())
                }
            }
            _isConnected.value = false
        }
    }
}
