package com.guesswhosesong.server.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.io.FileInputStream

/**
 * Initializes the Firebase Admin SDK once at startup.
 * Supports two auth strategies (in priority order):
 *  1. FIREBASE_SERVICE_ACCOUNT_PATH env var → reads JSON file from that path
 *  2. GOOGLE_APPLICATION_CREDENTIALS (standard ADC) → GoogleCredentials.getApplicationDefault()
 */
object FirebaseAdmin {

    private var initialized = false

    fun initialize() {
        if (initialized) return
        val credentials = buildCredentials()
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
        initialized = true
    }

    private fun buildCredentials(): GoogleCredentials {
        // 1. Check for raw JSON string
        val serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")
        if (!serviceAccountJson.isNullOrBlank()) {
            return GoogleCredentials.fromStream(serviceAccountJson.byteInputStream())
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        }

        // 2. Check for file path
        val serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
        if (!serviceAccountPath.isNullOrBlank() && File(serviceAccountPath).exists()) {
            return GoogleCredentials.fromStream(FileInputStream(serviceAccountPath))
                .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
        }

        // 3. Fallback to GOOGLE_APPLICATION_CREDENTIALS
        return GoogleCredentials.getApplicationDefault()
    }

    /**
     * Verifies a Firebase ID token and returns the user's uid.
     * Throws [IllegalArgumentException] if the token is invalid.
     */
    fun verifyIdToken(idToken: String): String {
        return FirebaseAuth.getInstance().verifyIdToken(idToken).uid
    }
}

