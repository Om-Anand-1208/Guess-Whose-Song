package com.guesswhosesong.app.data.player

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerIdentityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("gws_player_identity", Context.MODE_PRIVATE)

    @Synchronized
    fun getPlayerId(): String {
        var id = prefs.getString("player_id", null)
        if (id.isNullOrBlank()) {
            id = "player_" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString("player_id", id).apply()
        }
        return id
    }

    /**
     * For authentication against our server, the bearer token is simply the playerId.
     */
    fun getToken(): String = getPlayerId()
}
