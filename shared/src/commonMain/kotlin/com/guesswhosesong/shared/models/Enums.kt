package com.guesswhosesong.shared.models

import kotlinx.serialization.Serializable

@Serializable
enum class RoomState {
    LOBBY,
    SUBMISSION,
    PLAYING,
    RESULTS,
    ENDED
}

@Serializable
enum class RoundPhase {
    PLAYING_PREVIEW,
    VOTING,
    REVEALING
}

@Serializable
enum class RoundLengthPreset(val songsPerPlayer: Int) {
    QUICK(1),
    STANDARD(3),
    EXTENDED(20)
}

@Serializable
enum class VotingTimerOption(val seconds: Int) {
    TEN(10),
    FIFTEEN(15),
    TWENTY(20),
    THIRTY(30)
}
