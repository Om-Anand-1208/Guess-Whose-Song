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

    fun linkGoogle(onComplete: (Result<WebUser>) -> Unit) {
        firebaseLinkGoogle(
            resolve = { encoded ->
                val result = runCatching { GWSJson.decodeFromString<WebUser>(encoded) }
                result.onSuccess { _user.value = it }
                onComplete(result)
            },
            reject = { message ->
                val error = if (
                    message.contains("credential-already-in-use") ||
                    message.contains("email-already-in-use") ||
                    message.contains("provider-already-linked")
                ) {
                    IllegalStateException("That Google account is already linked to another player")
                } else {
                    IllegalStateException(message)
                }
                onComplete(Result.failure(error))
            }
        )
    }

    fun recoverGoogle(onComplete: (Result<WebUser>) -> Unit) {
        firebaseRecoverGoogle(
            resolve = { encoded ->
                val result = runCatching { GWSJson.decodeFromString<WebUser>(encoded) }
                result.onSuccess {
                    _user.value = it
                    _status.value = AuthStatus.READY
                    _error.value = null
                }
                onComplete(result)
            },
            reject = { message -> onComplete(Result.failure(IllegalStateException(message))) }
        )
    }
}
