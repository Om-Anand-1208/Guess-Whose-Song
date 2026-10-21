package com.guesswhosesong.server.music

import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.SpotifyCategory
import com.guesswhosesong.shared.models.SpotifySuggestion
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MusicServiceArtworkTest {
    @Test
    fun `a failed catalog request is not cached as no matches`() = runBlocking {
        var calls = 0
        val catalog = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
                calls++
                if (calls == 1) throw MusicCatalogUnavailableException()
                return listOf(track("itunes-1", "https://art.example/cover.jpg"))
            }
            override fun close() = Unit
        }
        val service = MusicService(catalog)

        assertThrows(MusicCatalogUnavailableException::class.java) {
            runBlocking { service.search("Blue Monday") }
        }
        assertEquals("itunes-1", service.search("Blue Monday").single().id)
        assertEquals(2, calls)
    }

    @Test
    fun `search returns tracks and artwork from the configured iTunes catalog`() = runBlocking {
        val itunes = StubCatalog(listOf(track("itunes-1", "https://art.example/cover.jpg")))

        val result = MusicService(itunes).search("Blue Monday")

        assertEquals("itunes-1", result.single().id)
        assertEquals("https://art.example/cover.jpg", result.single().albumArtUrl)
    }

    @Test
    fun `resolveEntry fills missing artwork from iTunes`() = runBlocking {
        val itunes = StubCatalog(listOf(track("itunes-1", "https://art.example/cover.jpg")))
        val entry = SongEntry(
            songId = "submitted-id",
            title = "Blue Monday",
            artist = "New Order",
            previewUrl = "https://preview.example/song.mp3"
        )

        val resolved = MusicService(itunes).resolveEntry(entry)

        assertEquals("itunes-1", resolved?.songId)
        assertEquals("https://art.example/cover.jpg", resolved?.albumArtUrl)
        assertEquals("https://preview.example/song.mp3", resolved?.previewUrl)
        assertEquals("Rock", MusicService(itunes).resolveEntryWithGenre(entry)?.genreName)
    }

    @Test
    fun `resolveEntry returns null when iTunes has no close match`() = runBlocking {
        val service = MusicService(StubCatalog(emptyList()))

        assertNull(service.resolveEntry(SongEntry("submitted", "Unknown", "Nobody", previewUrl = "")))
    }

    @Test
    fun `Spotify tracks enter the automatic pool only when title artist and album match`() = runBlocking {
        val catalog = StubCatalog(listOf(
            track("wrong-title", "").copy(title = "Blue Tuesday", albumTitle = "Power Corruption and Lies"),
            track("wrong-artist", "").copy(artist = "Another Order", albumTitle = "Power Corruption and Lies"),
            track("wrong-album", "").copy(albumTitle = "Substance"),
            track("exact", "https://art.example/itunes.jpg").copy(albumTitle = "Power, Corruption & Lies")
        ))
        val spotify = SpotifySuggestion(
            title = "Blue Monday",
            artist = "New Order",
            albumArtUrl = "https://art.example/spotify.jpg",
            category = SpotifyCategory.RECENTLY_PLAYED,
            albumTitle = "Power Corruption Lies"
        )

        val resolved = MusicService(catalog).resolveSpotifyTrackWithGenre(spotify)

        assertEquals("exact", resolved?.song?.songId)
        assertEquals("Rock", resolved?.genreName)
        assertEquals("https://art.example/spotify.jpg", resolved?.song?.albumArtUrl)
    }

    @Test
    fun `Spotify track without the same iTunes album is rejected`() = runBlocking {
        val catalog = StubCatalog(listOf(track("wrong-album", "").copy(albumTitle = "Substance")))
        val service = MusicService(catalog)

        assertNull(service.resolveSpotifyTrackWithGenre(
            SpotifySuggestion("Blue Monday", "New Order", category = SpotifyCategory.TOP_TRACKS, albumTitle = "Power Corruption and Lies")
        ))
        assertNull(service.resolveSpotifyTrackWithGenre(
            SpotifySuggestion("Blue Monday", "New Order", category = SpotifyCategory.TOP_TRACKS, albumTitle = "")
        ))
    }

    @Test
    fun `genre matches add one new artist track per submitted genre`() = runBlocking {
        fun candidate(id: String, artist: String, genre: String) = TrackSearchResult(
            id, "Song $id", artist, "", "https://preview.example/$id.mp3", genre
        )
        val catalog = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = when (query) {
                "Rock" -> listOf(
                    candidate("wrong", "Other", "Pop"),
                    candidate("same-artist", "Artist A", "Rock"),
                    candidate("rock-1", "Band One", "Rock"),
                    candidate("rock-2", "Band Two", "Rock")
                )
                "Classical" -> listOf(candidate("classical-1", "Composer", "Classical"))
                "Pop" -> listOf(candidate("pop-1", "Singer", "Pop"))
                else -> emptyList()
            }
            override fun close() = Unit
        }
        val submitted = listOf(
            SongEntry("submitted-1", "First", "Artist A", previewUrl = "https://preview.example/first.mp3"),
            SongEntry("submitted-2", "Second", "Artist B", previewUrl = "https://preview.example/second.mp3")
        )

        val matches = MusicService(catalog).getGenreMatchedTracks(
            submitted, listOf("Rock", "Rock", "Classical", "Pop")
        )

        assertEquals(4, matches.size)
        assertEquals(2, matches.count { it.genreName == "Rock" })
        assertEquals(4, matches.map { it.id }.distinct().size)
        assertTrue(matches.none { it.artist == "Artist A" || it.artist == "Artist B" })
    }

    private fun track(id: String, art: String) = TrackSearchResult(
        id = id,
        title = "Blue Monday",
        artist = "New Order",
        albumArtUrl = art,
        previewUrl = "https://preview.example/song.mp3",
        genreName = "Rock"
    )

    private class StubCatalog(private val results: List<TrackSearchResult>) : MusicCatalogClient {
        override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = results.take(limit)
        override fun close() = Unit
    }
}
