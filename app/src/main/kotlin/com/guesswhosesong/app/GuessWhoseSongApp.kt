package com.guesswhosesong.app

import android.app.Application
import com.guesswhosesong.app.data.logging.CrashLogger
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GuessWhoseSongApp : Application() {

    @Inject
    lateinit var playerIdentityManager: PlayerIdentityManager

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this, playerIdentityManager)
    }
}
