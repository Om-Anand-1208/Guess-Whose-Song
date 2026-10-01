package com.guesswhosesong.server.music

import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MusicServiceArtworkTest {
    @Test
    fun `search fills missing Deezer artwork from matching iTunes result`() = runBlocking {
        val deezer = StubCatalog(
            listOf(track(id = "deezer-1", art = ""))
        )
        val itunes = StubCatalog(
            listOf(track(id = "itunes-1", art = "https://art.example/cover.jpg"))
        )

        val result = MusicService(deezer, itunes).search("Blue Monday")

        assertEquals("deezer-1", result.single().id)
        assertEquals("https://art.example/cover.jpg", result.single().albumArtUrl)
    }

    @Test
    fun `resolveEntry preserves artwork already attached by the user`() = runBlocking {
        val deezer = StubCatalog(
            listOf(track(id = "deezer-1", art = "https://art.example/deezer.jpg"))
        )
        val itunes = StubCatalog(emptyList())
        val entry = SongEntry(
            songId = "submitted-id",
            title = "Blue Monday",
            artist = "New Order",
            albumArtUrl = "https://art.example/original.jpg",
            previewUrl = "https://preview.example/song.mp3"
        )

        val resolved = MusicService(deezer, itunes).resolveEntry(entry)

        assertEquals("deezer-1", resolved?.songId)
        assertEquals("https://art.example/original.jpg", resolved?.albumArtUrl)
    }

    private fun track(id: String, art: String) = TrackSearchResult(
        id = id,
        title = "Blue Monday",
        artist = "New Order",
        albumArtUrl = art,
        previewUrl = "https://preview.example/song.mp3"
    )

    private class StubCatalog(private val results: List<TrackSearchResult>) : MusicCatalogClient {
        override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = results.take(limit)
        override fun close() = Unit
    }
}
