package com.guesswhosesong.server.auth

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayInputStream
import java.io.FileInputStream

data class AuthenticatedUser(
    val uid: String,
    val isAnonymous: Boolean
)

class InvalidFirebaseTokenException : Exception("Invalid authentication token")

/** Verifies Firebase ID tokens and exposes only the stable UID to the game. */
class FirebaseTokenVerifier private constructor(
    private val firebaseAuth: FirebaseAuth
) {
    fun verifyBearerHeader(header: String?): AuthenticatedUser {
        val token = header
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw InvalidFirebaseTokenException()

        return try {
            val decoded = firebaseAuth.verifyIdToken(token)
            val firebaseClaims = decoded.claims["firebase"] as? Map<*, *>
            val provider = firebaseClaims?.get("sign_in_provider") as? String
            AuthenticatedUser(
                uid = decoded.uid,
                isAnonymous = provider == "anonymous"
            )
        } catch (_: Exception) {
            throw InvalidFirebaseTokenException()
        }
    }

    companion object {
        fun fromEnvironment(): FirebaseTokenVerifier {
            val app = FirebaseApp.getApps().firstOrNull() ?: run {
                val json = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")
                val path = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
                    ?: System.getenv("GOOGLE_APPLICATION_CREDENTIALS")

                val credentials = when {
                    !json.isNullOrBlank() -> GoogleCredentials.fromStream(
                        ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
                    )
                    !path.isNullOrBlank() -> FileInputStream(path).use { input ->
                        GoogleCredentials.fromStream(input)
                    }
                    else -> error(
                        "Firebase credentials are not configured. Set FIREBASE_SERVICE_ACCOUNT_JSON " +
                            "or FIREBASE_SERVICE_ACCOUNT_PATH."
                    )
                }

                val projectId = System.getenv("FIREBASE_PROJECT_ID")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "guess-whose-song"

                FirebaseApp.initializeApp(
                    FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(projectId)
                        .build()
                )
            }
            return FirebaseTokenVerifier(FirebaseAuth.getInstance(app))
        }

        fun fromFirebaseAuth(firebaseAuth: FirebaseAuth): FirebaseTokenVerifier =
            FirebaseTokenVerifier(firebaseAuth)
    }
}
