package com.guesswhosesong.server.engine

import com.guesswhosesong.server.security.InputValidation
import com.guesswhosesong.shared.models.Player
import com.guesswhosesong.shared.models.RoundCountRules
import com.guesswhosesong.shared.models.SongEntry

/** Server-side validation for a player's bounded song submission. */
object SubmissionRules {
    fun playersMissingSongs(players: List<Player>): List<String> = players
        .filterNot { it.isComputer }
        .filter { it.pendingSongs.isEmpty() && it.pendingSong == null }
        .map { it.id }

    fun isValid(songs: List<SongEntry>, roundCount: Int, playerCount: Int): Boolean {
        val limit = RoundCountRules.maxSongsPerPlayer(roundCount, playerCount)
        return songs.isNotEmpty() &&
            songs.size <= limit &&
            songs.all(InputValidation::song) &&
            songs.map { it.songId.trim().lowercase() }.distinct().size == songs.size &&
            songs.map { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }.distinct().size == songs.size
    }
}
