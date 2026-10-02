package com.guesswhosesong.shared.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Shared JSON configuration for client actions and server events.
 * Use this instance everywhere for consistency.
 */
val GWSJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

fun ServerMessage.toJson(): String = GWSJson.encodeToString(this)

fun String.toServerMessage(): ServerMessage = GWSJson.decodeFromString(this)

fun ClientMessage.toJson(): String = GWSJson.encodeToString(this)

fun String.toClientMessage(): ClientMessage = GWSJson.decodeFromString(this)
