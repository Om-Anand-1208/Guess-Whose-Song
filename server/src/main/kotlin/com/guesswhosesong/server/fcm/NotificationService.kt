package com.guesswhosesong.server.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory

object NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    /**
     * Notify a specific player's device that the room is starting.
     * [fcmToken] is the device FCM registration token stored server-side.
     */
    fun sendRoomStarting(fcmToken: String, roomCode: String) {
        if (fcmToken.isBlank()) return
        send(
            token = fcmToken,
            title = "Game is starting!",
            body = "Room $roomCode is starting now. Open the app to play!",
            data = mapOf("type" to "ROOM_STARTING", "roomCode" to roomCode)
        )
    }

    /**
     * Notify a player they've been promoted to host.
     */
    fun sendHostTransferred(fcmToken: String, roomCode: String) {
        if (fcmToken.isBlank()) return
        send(
            token = fcmToken,
            title = "You are now the host!",
            body = "The previous host disconnected. You're in charge of room $roomCode.",
            data = mapOf("type" to "HOST_TRANSFERRED", "roomCode" to roomCode)
        )
    }

    private fun send(token: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()
            val response = FirebaseMessaging.getInstance().send(message)
            logger.debug("FCM sent: $response")
        } catch (e: Exception) {
            logger.warn("FCM send failed: ${e.message}")
        }
    }
}
