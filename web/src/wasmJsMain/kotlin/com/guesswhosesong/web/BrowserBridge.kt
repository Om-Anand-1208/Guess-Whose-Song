@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.js

@Serializable
internal data class BrowserHttpEnvelope(val status: Int, val body: String)

internal fun configuredApiBaseUrl(): String = js(
    "window.GWS_RUNTIME_CONFIG?.apiBaseUrl || 'http://localhost:8080'"
)

internal fun firebaseConfigJson(): String = js("JSON.stringify(window.GWS_FIREBASE_CONFIG || {})")

private fun currentEpochMillisNumber(): Double = js("Date.now()")

internal fun currentEpochMillis(): Long = currentEpochMillisNumber().toLong()

internal fun sessionGet(key: String): String? = js("window.sessionStorage.getItem(key)")

internal fun sessionSet(key: String, value: String) {
    js("window.sessionStorage.setItem(key, value)")
}

internal fun localGet(key: String): String? = js("window.localStorage.getItem(key)")

internal fun localSet(key: String, value: String) {
    js("window.localStorage.setItem(key, value)")
}

internal fun systemPrefersDark(): Boolean = js("window.matchMedia('(prefers-color-scheme: dark)').matches")

internal fun copyTextToClipboard(text: String) {
    js("navigator.clipboard?.writeText(text).catch(() => {})")
}

internal fun reportAlbumArtFailure(url: String) {
    js("console.warn('Album artwork request failed:', url)")
}

internal fun openExternal(url: String) {
    js("{ const opened = window.open(url, '_blank', 'noopener,noreferrer'); if (!opened) window.location.assign(url); }")
}

internal fun playPreview(url: String) {
    js("{ window.__gwsAudio = window.__gwsAudio || new Audio(); window.__gwsAudio.src = url; window.__gwsAudio.play().catch(() => {}); }")
}

internal fun stopPreview() {
    js("{ if (window.__gwsAudio) { window.__gwsAudio.pause(); window.__gwsAudio.currentTime = 0; } }")
}

internal fun previewPlaybackPositionSeconds(): Double = js(
    "window.__gwsAudio ? (Number(window.__gwsAudio.currentTime) || 0) : 0"
)

internal fun previewPlaybackDurationSeconds(): Double = js(
    "window.__gwsAudio && Number.isFinite(window.__gwsAudio.duration) ? window.__gwsAudio.duration : 0"
)

private fun fetchText(
    url: String,
    token: String,
    method: String,
    body: String,
    resolve: (String) -> Unit,
    reject: (String) -> Unit
) {
    js("""
        const headers = token ? { 'Authorization': 'Bearer ' + token } : {};
        // ngrok's free tunnel can return its browser-warning HTML page to fetch
        // requests. This header lets API calls pass through to the local Ktor app.
        const apiHost = new URL(url).hostname;
        if (apiHost.endsWith('.ngrok-free.dev') || apiHost.endsWith('.ngrok.io')) {
            headers['ngrok-skip-browser-warning'] = 'true';
        }
        if (method !== 'GET' && method !== 'HEAD') headers['Content-Type'] = 'application/json';
        fetch(url, {
            method: method,
            headers: headers,
            body: body || undefined,
            mode: 'cors',
            credentials: 'omit'
        }).then(async response => {
            resolve(JSON.stringify({ status: response.status, body: await response.text() }));
        }).catch(error => reject(String(error && error.message ? error.message : error)));
    """)
}

internal suspend fun browserFetch(
    url: String,
    token: String,
    method: String = "GET",
    body: String = ""
): BrowserHttpEnvelope = suspendCancellableCoroutine { continuation ->
    fetchText(url, token, method, body,
        resolve = { encoded ->
            runCatching { GWSJson.decodeFromString<BrowserHttpEnvelope>(encoded) }
                .onSuccess { continuation.resume(it) }
                .onFailure { continuation.resumeWithException(it) }
        },
        reject = { message -> continuation.resumeWithException(IllegalStateException(message)) }
    )
}

private fun firebaseBootstrap(
    configJson: String,
    resolve: (String) -> Unit,
    reject: (String) -> Unit
) {
    js("""
        try {
            const config = JSON.parse(configJson);
            if (!config.apiKey || !config.projectId || !config.appId) {
                throw new Error('Firebase web configuration is missing');
            }
            if (!window.firebase.apps.length) window.firebase.initializeApp(config);
            const auth = window.firebase.auth();
            const ready = auth.currentUser ? Promise.resolve(auth.currentUser) : auth.signInAnonymously().then(result => result.user);
            ready.then(user => resolve(JSON.stringify({
                uid: user.uid,
                isAnonymous: user.isAnonymous,
                isGoogleLinked: (user.providerData || []).some(profile => profile.providerId === 'google.com'),
                email: user.email || ''
            })))
                .catch(error => reject(String(error.code || error.message || error)));
        } catch (error) {
            reject(String(error && error.message ? error.message : error));
        }
    """)
}

internal suspend fun firebaseStart(): String = suspendCancellableCoroutine { continuation ->
    firebaseBootstrap(
        configJson = firebaseConfigJson(),
        resolve = { continuation.resume(it) },
        reject = { continuation.resumeWithException(IllegalStateException(it)) }
    )
}

private fun firebaseToken(
    forceRefresh: Boolean,
    resolve: (String) -> Unit,
    reject: (String) -> Unit
) {
    js("""
        try {
            const user = window.firebase.auth().currentUser;
            if (!user) throw new Error('Firebase user is not available');
            user.getIdToken(forceRefresh).then(token => resolve(token))
                .catch(error => reject(String(error.code || error.message || error)));
        } catch (error) {
            reject(String(error && error.message ? error.message : error));
        }
    """)
}

internal suspend fun firebaseIdToken(forceRefresh: Boolean): String = suspendCancellableCoroutine { continuation ->
    firebaseToken(forceRefresh,
        resolve = { continuation.resume(it) },
        reject = { continuation.resumeWithException(IllegalStateException(it)) }
    )
}

private fun firebaseGoogleOperation(
    mode: String,
    resolve: (String) -> Unit,
    reject: (String) -> Unit
) {
    js("""
        const errorMessage = error => {
            const code = error && error.code ? String(error.code) : '';
            const message = error && error.message ? String(error.message) : String(error);
            return code && !message.includes(code) ? code + ': ' + message : message;
        };
        const encodeUser = user => JSON.stringify({
            uid: user.uid,
            isAnonymous: user.isAnonymous,
            isGoogleLinked: (user.providerData || []).some(profile => profile.providerId === 'google.com'),
            email: user.email || ''
        });
        try {
            const auth = window.firebase.auth();
            const provider = new window.firebase.auth.GoogleAuthProvider();
            const currentUser = auth.currentUser;
            if ((mode === 'link' || mode === 'recover') && !currentUser) throw new Error('Firebase user is not available');
            if (mode === 'recover' && (currentUser.providerData || []).some(profile => profile.providerId === 'google.com')) {
                throw new Error('Google is already connected to this player');
            }
            let operation;
            if (mode === 'link') {
                operation = currentUser.linkWithPopup(provider);
            } else if (mode === 'recover') {
                operation = currentUser.linkWithPopup(provider).then(() =>
                    currentUser.unlink(provider.providerId).then(
                        () => Promise.reject(new Error('No existing player is linked to this Google account')),
                        error => Promise.reject(new Error('No existing player was found. The temporary Google link could not be removed: ' + errorMessage(error)))
                    )
                ).catch(error => {
                    if (![
                        'auth/credential-already-in-use',
                        'auth/email-already-in-use',
                        'auth/account-exists-with-different-credential'
                    ].includes(error && error.code)) throw error;
                    const credential = window.firebase.auth.GoogleAuthProvider.credentialFromError(error);
                    if (!credential) throw error;
                    return auth.signInWithCredential(credential);
                });
            } else {
                operation = auth.signInWithPopup(provider);
            }
            operation.then(result => {
                const user = result.user || result;
                resolve(encodeUser(user));
            }).catch(error => reject(errorMessage(error)));
        } catch (error) {
            reject(errorMessage(error));
        }
    """)
}

internal fun firebaseLinkGoogle(resolve: (String) -> Unit, reject: (String) -> Unit) =
    firebaseGoogleOperation("link", resolve, reject)

internal fun firebaseRecoverGoogle(resolve: (String) -> Unit, reject: (String) -> Unit) =
    firebaseGoogleOperation("recover", resolve, reject)

internal fun firebaseDisconnectGoogle(resolve: (String) -> Unit, reject: (String) -> Unit) {
    js("""
        try {
            const auth = window.firebase.auth();
            const user = auth.currentUser;
            if (!user) throw new Error('Firebase user is not available');
            user.unlink('google.com').then(updatedUser => resolve(JSON.stringify({
                uid: updatedUser.uid,
                isAnonymous: updatedUser.isAnonymous,
                isGoogleLinked: (updatedUser.providerData || []).some(profile => profile.providerId === 'google.com'),
                email: updatedUser.email || ''
            }))).catch(error => reject(String(error.code || error.message || error)));
        } catch (error) {
            reject(String(error && error.message ? error.message : error));
        }
    """)
}
