package com.guesswhosesong.app

import android.net.Uri
import java.util.Locale

object RoomInviteLinks {
    private const val HOST = "guess-whose-song.web.app"
    private val codePattern = Regex("[A-Z0-9]{6}")

    fun urlFor(roomCode: String): String =
        "https://$HOST/join/${roomCode.uppercase(Locale.ROOT)}"

    fun codeFrom(uri: Uri?): String? {
        if (uri?.scheme != "https" || uri.host != HOST || uri.pathSegments.size != 2 ||
            uri.pathSegments[0] != "join") return null
        val code = uri.pathSegments[1].uppercase(Locale.ROOT)
        return code.takeIf(codePattern::matches)
    }
}
