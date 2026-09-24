package com.guesswhosesong.app.data.logging

import android.content.Context
import android.os.Build
import com.guesswhosesong.app.data.player.PlayerIdentityManager
import com.guesswhosesong.app.di.NetworkModule
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object CrashLogger {

    fun install(context: Context, playerIdentityManager: PlayerIdentityManager) {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                sendCrashReport(context, playerIdentityManager, thread, throwable)
            } catch (_: Throwable) {
                // Ignore reporting failure to ensure default handler proceeds
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun sendCrashReport(
        context: Context,
        playerIdentityManager: PlayerIdentityManager,
        thread: Thread,
        throwable: Throwable
    ) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val playerId = try { playerIdentityManager.getPlayerId() } catch (_: Exception) { "unknown" }
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"
        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "unknown"
        } catch (_: Exception) { "unknown" }

        val json = JSONObject().apply {
            put("playerId", playerId)
            put("device", device)
            put("appVersion", appVersion)
            put("exceptionClass", throwable::class.java.name)
            put("message", throwable.message ?: "No message")
            put("stackTrace", stackTrace)
        }.toString()

        val worker = thread(start = true) {
            try {
                val url = URL("${NetworkModule.BASE_URL}/api/logs/crash")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { os ->
                    OutputStreamWriter(os, "UTF-8").use { osw ->
                        osw.write(json)
                        osw.flush()
                    }
                }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
            }
        }
        worker.join(3500)
    }
}
