package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
data class WebUser(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String = ""
)

enum class AuthStatus { LOADING, READY, ERROR }

class WebAuthManager {
    private val _status = MutableStateFlow(AuthStatus.LOADING)
    val status: StateFlow<AuthStatus> = _status.asStateFlow()
    private val _user = MutableStateFlow<WebUser?>(null)
    val user: StateFlow<WebUser?> = _user.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun start() {
        _status.value = AuthStatus.LOADING
        runCatching { GWSJson.decodeFromString<WebUser>(firebaseStart()) }
            .onSuccess {
                _user.value = it
                _status.value = AuthStatus.READY
                _error.value = null
            }
            .onFailure {
                _status.value = AuthStatus.ERROR
                _error.value = it.message ?: "Firebase authentication failed"
            }
    }

    suspend fun idToken(forceRefresh: Boolean = false): String {
        if (_status.value != AuthStatus.READY) error(_error.value ?: "Authentication is not ready")
        return firebaseIdToken(forceRefresh).also { check(it.isNotBlank()) }
    }

    suspend fun linkGoogle(): Result<WebUser> = runCatching {
        val linked = GWSJson.decodeFromString<WebUser>(firebaseLinkGoogle())
        _user.value = linked
        linked
    }.recoverCatching { throwable ->
        val message = throwable.message.orEmpty()
        if (message.contains("credential-already-in-use") ||
            message.contains("email-already-in-use") ||
            message.contains("provider-already-linked")
        ) {
            throw IllegalStateException("That Google account is already linked to another player")
        }
        throw throwable
    }

    suspend fun recoverGoogle(): Result<WebUser> = runCatching {
        val recovered = GWSJson.decodeFromString<WebUser>(firebaseRecoverGoogle())
        _user.value = recovered
        _status.value = AuthStatus.READY
        recovered
    }
}
