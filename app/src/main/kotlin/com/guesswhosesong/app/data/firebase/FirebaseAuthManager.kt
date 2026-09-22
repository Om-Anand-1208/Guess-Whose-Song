package com.guesswhosesong.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages Firebase Anonymous Authentication.
 * Signs in the user anonymously on first launch and returns a stable UID.
 */
class FirebaseAuthManager(private val auth: FirebaseAuth) {

    val currentUser: FirebaseUser? get() = auth.currentUser
    val currentUid: String? get() = auth.currentUser?.uid

    /**
     * Signs in anonymously if not already signed in.
     * @return the Firebase UID
     */
    suspend fun signInAnonymously(): String {
        if (auth.currentUser != null) return auth.currentUser!!.uid
        return suspendCancellableCoroutine { cont ->
            auth.signInAnonymously()
                .addOnSuccessListener { result -> cont.resume(result.user!!.uid) }
                .addOnFailureListener { exception -> cont.resumeWithException(exception) }
        }
    }

    /**
     * Gets a fresh Firebase ID token for authenticating with the backend.
     */
    suspend fun getIdToken(): String {
        val user = auth.currentUser ?: error("Not signed in")
        return suspendCancellableCoroutine { cont ->
            user.getIdToken(false)
                .addOnSuccessListener { result -> cont.resume(result.token ?: "") }
                .addOnFailureListener { exception -> cont.resumeWithException(exception) }
        }
    }
}
