@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.guesswhosesong.web

import com.guesswhosesong.shared.dto.GWSJson
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.js

@Serializable
internal data class BrowserHttpEnvelope(val status: Int, val body: String)

internal fun configuredApiBaseUrl(): String = js(
    "window.GWS_RUNTIME_CONFIG?.apiBaseUrl || 'http://localhost:8080'"
)

internal fun configuredWsBaseUrl(): String = js(
    "window.GWS_RUNTIME_CONFIG?.wsBaseUrl || (window.location.protocol === 'https:' ? 'wss://' + window.location.host : 'ws://' + window.location.host)"
)

internal fun firebaseConfigJson(): String = js("JSON.stringify(window.GWS_FIREBASE_CONFIG || {})")

private fun currentEpochMillisNumber(): Double = js("Date.now()")

internal fun currentEpochMillis(): Long = currentEpochMillisNumber().toLong()

internal fun sessionGet(key: String): String? = js("window.sessionStorage.getItem(key)")

internal fun sessionSet(key: String, value: String) {
    js("window.sessionStorage.setItem(key, value)")
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
            ready.then(user => resolve(JSON.stringify({ uid: user.uid, isAnonymous: user.isAnonymous, email: user.email || '' })))
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
    link: Boolean,
    resolve: (String) -> Unit,
    reject: (String) -> Unit
) {
    js("""
        try {
            const auth = window.firebase.auth();
            const provider = new window.firebase.auth.GoogleAuthProvider();
            const operation = link ? auth.currentUser.linkWithPopup(provider) : auth.signInWithPopup(provider);
            operation.then(result => {
                const user = result.user || result;
                resolve(JSON.stringify({ uid: user.uid, isAnonymous: user.isAnonymous, email: user.email || '' }));
            }).catch(error => reject(String(error.code || error.message || error)));
        } catch (error) {
            reject(String(error && error.message ? error.message : error));
        }
    """)
}

internal suspend fun firebaseLinkGoogle(): String = suspendCancellableCoroutine { continuation ->
    firebaseGoogleOperation(true,
        resolve = { continuation.resume(it) },
        reject = { continuation.resumeWithException(IllegalStateException(it)) }
    )
}

internal suspend fun firebaseRecoverGoogle(): String = suspendCancellableCoroutine { continuation ->
    firebaseGoogleOperation(false,
        resolve = { continuation.resume(it) },
        reject = { continuation.resumeWithException(IllegalStateException(it)) }
    )
}

private fun openSocket(
    url: String,
    protocol: String,
    ticketProtocol: String,
    onOpen: () -> Unit,
    onMessage: (String) -> Unit,
    onClose: (Int, String) -> Unit,
    onError: () -> Unit
): JsAny = js("""
    (() => {
        const socket = new WebSocket(url, [protocol, ticketProtocol]);
        socket.onopen = () => onOpen();
        socket.onmessage = event => onMessage(String(event.data));
        socket.onclose = event => onClose(event.code, String(event.reason || ''));
        socket.onerror = () => onError();
        return socket;
    })()
""")

internal fun browserOpenSocket(
    url: String,
    protocol: String,
    ticketProtocol: String,
    onOpen: () -> Unit,
    onMessage: (String) -> Unit,
    onClose: (Int, String) -> Unit,
    onError: () -> Unit
): JsAny = openSocket(url, protocol, ticketProtocol, onOpen, onMessage, onClose, onError)

internal fun browserSendSocket(socket: JsAny, text: String) {
    js("socket.send(text)")
}

internal fun browserCloseSocket(socket: JsAny, code: Int, reason: String) {
    js("socket.close(code, reason)")
}
