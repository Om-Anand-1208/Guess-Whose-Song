package com.guesswhosesong.shared.models

import kotlinx.serialization.Serializable

@Serializable
data class RoomChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val sentAtEpochMillis: Long
)
