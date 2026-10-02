package com.guesswhosesong.app.data.player

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AccountLinkResult {
    data object Linked : AccountLinkResult()
    data object SignedIn : AccountLinkResult()
    data object Collision : AccountLinkResult()
    data class Failed(val message: String) : AccountLinkResult()
}

/** Owns the Firebase identity used for all server authentication. */
@Singleton
class PlayerIdentityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Guest access is automatic; no registration screen is required.
        scope.launch { runCatching { ensureSignedIn() } }
    }

    suspend fun ensureSignedIn(): FirebaseUser {
        auth.currentUser?.let { return it }
        return auth.signInAnonymously().await().user
            ?: error("Firebase did not return an anonymous user")
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): String {
        return ensureSignedIn().getIdToken(forceRefresh).await().token
            ?: error("Firebase did not return an ID token")
    }

    fun currentUid(): String? = auth.currentUser?.uid

    fun isAccountLinked(): Boolean = auth.currentUser?.providerData
        ?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true

    fun googleSignInIntent(activity: Activity): Intent {
        val clientIdResource = activity.resources.getIdentifier(
            "default_web_client_id", "string", activity.packageName
        )
        require(clientIdResource != 0) { "Google web client ID is not configured" }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(clientIdResource))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, options).signInIntent
    }

    /** Links Google to the current anonymous Firebase user; it never merges users. */
    suspend fun linkGoogle(data: Intent): AccountLinkResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val idToken = account.idToken ?: return AccountLinkResult.Failed("Google did not return an ID token")
            val user = ensureSignedIn()
            user.linkWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            AccountLinkResult.Linked
        } catch (_: FirebaseAuthUserCollisionException) {
            AccountLinkResult.Collision
        } catch (e: Exception) {
            AccountLinkResult.Failed(e.message ?: "Google linking failed")
        }
    }

    suspend fun disconnectGoogle() {
        val user = auth.currentUser ?: return
        user.unlink(GoogleAuthProvider.PROVIDER_ID).await()
    }

    /** Signs in only when the Google credential already belongs to another Firebase player. */
    suspend fun signInWithGoogle(data: Intent): AccountLinkResult {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
            val idToken = account.idToken ?: return AccountLinkResult.Failed("Google did not return an ID token")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            if (isAccountLinked()) {
                return AccountLinkResult.Failed("Google is already connected to this player")
            }
            val currentUser = ensureSignedIn()
            try {
                currentUser.linkWithCredential(credential).await()
                currentUser.unlink(GoogleAuthProvider.PROVIDER_ID).await()
                AccountLinkResult.Failed("No existing player is linked to this Google account")
            } catch (_: FirebaseAuthUserCollisionException) {
                auth.signInWithCredential(credential).await()
                AccountLinkResult.SignedIn
            }
        } catch (e: Exception) {
            AccountLinkResult.Failed(e.message ?: "Google sign-in failed")
        }
    }
}
