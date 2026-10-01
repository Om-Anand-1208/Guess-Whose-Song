package com.guesswhosesong.app.ui.screens.join

import android.net.Uri

/** Converts a scanned QR payload into the room code accepted by the Join screen. */
internal object JoinCodeParser {
    private val roomCodePattern = Regex("^[A-Za-z0-9]{6}$")
    private val queryKeys = setOf("code", "room", "roomcode", "joincode")

    fun parse(payload: String?): String? {
        val value = payload?.trim()?.takeIf(String::isNotEmpty) ?: return null
        normalizeCode(value)?.let { return it }

        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
        val queryValues = runCatching {
            uri.queryParameterNames
                .filter { it.lowercase() in queryKeys }
                .mapNotNull(uri::getQueryParameter)
        }.getOrDefault(emptyList())
        queryValues.firstNotNullOfOrNull(::normalizeCode)?.let { return it }
        return normalizeCode(uri.lastPathSegment)
    }

    private fun normalizeCode(value: String?): String? = value
        ?.trim()
        ?.takeIf(roomCodePattern::matches)
        ?.uppercase()
}
