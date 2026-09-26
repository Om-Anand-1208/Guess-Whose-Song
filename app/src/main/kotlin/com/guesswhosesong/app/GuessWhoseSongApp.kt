package com.guesswhosesong.app

import android.app.Application
import com.guesswhosesong.app.data.logging.CrashLogger
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GuessWhoseSongApp : Application() {

    @Inject
    lateinit var playerIdentityManager: PlayerIdentityManager

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { playerIdentityManager.ensureSignedIn() }
        }
        CrashLogger.install(this)
    }
}
