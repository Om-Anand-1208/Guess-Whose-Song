package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.SpotifyCategory
import com.guesswhosesong.shared.models.SpotifySuggestion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RecentTrackDeduplicatorTest {
    private fun played(
        playerId: String,
        title: String,
        artist: String,
        spotifyTrackId: String? = null
    ) = PlayerRecentTrack(
        playerId,
        SpotifySuggestion(title, artist, category = SpotifyCategory.RECENTLY_PLAYED, spotifyTrackId = spotifyTrackId)
    )

    @Test
    fun `shared Spotify track is removed for both players`() {
        val result = RecentTrackDeduplicator.uniqueToOnePlayer(
            listOf(
                played("p1", "Song", "Artist", "track-1"),
                played("p2", "Same song metadata", "Another title artist", "track-1"),
                played("p1", "Private song", "Artist", "track-2")
            )
        )

        assertEquals(listOf("track-2"), result.map { it.track.spotifyTrackId })
    }

    @Test
    fun `missing IDs fall back to normalized title and primary artist`() {
        val result = RecentTrackDeduplicator.uniqueToOnePlayer(
            listOf(
                played("p1", "  Same-Song! ", "The Artist", "spotify-id"),
                played("p2", "same song", "the artist")
            )
        )

        assertEquals(emptyList<PlayerRecentTrack>(), result)
    }

    @Test
    fun `different known IDs remain separate even when metadata matches`() {
        val result = RecentTrackDeduplicator.uniqueToOnePlayer(
            listOf(
                played("p1", "Song", "Artist", "recording-1"),
                played("p2", "Song", "Artist", "recording-2")
            )
        )

        assertEquals(listOf("p1", "p2"), result.map { it.playerId })
    }

    @Test
    fun `repeat listens by one player collapse to one entry`() {
        val result = RecentTrackDeduplicator.uniqueToOnePlayer(
            listOf(
                played("p1", "Song", "Artist", "track-1"),
                played("p1", "Song again", "Other metadata", "track-1")
            )
        )

        assertEquals(1, result.size)
        assertEquals("track-1", result.single().track.spotifyTrackId)
    }
}
