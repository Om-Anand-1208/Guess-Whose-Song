package com.guesswhosesong.server.music

import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FallbackMusicCatalogClientTest {
    private val song = TrackSearchResult("audius:abc12", "Song", "Artist (Audius)", "https://art.example/a.jpg", "https://api.audius.co/v1/tracks/abc12/stream", "Rock")

    @Test fun `falls back after provider failure or an empty response`() = runBlocking {
        var secondaryCalls = 0
        val secondary = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> { secondaryCalls++; return listOf(song) }
            override fun close() = Unit
        }
        val broken = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = throw MusicCatalogUnavailableException()
            override fun close() = Unit
        }
        assertEquals(song, FallbackMusicCatalogClient(listOf(broken, secondary)).search("song").single())
        val empty = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = emptyList()
            override fun close() = Unit
        }
        assertEquals(song, FallbackMusicCatalogClient(listOf(empty, secondary)).search("song").single())
        assertTrue(FallbackMusicCatalogClient(listOf(empty, broken)).search("song").isEmpty())
        assertEquals(2, secondaryCalls)
    }

    @Test fun `Audius maps playable public tracks but excludes gated or unstreamable tracks`() {
        fun parse(flags: String, duration: Int = 240) = Json.parseToJsonElement("""{"id":"abc12","title":"Song","duration":$duration,"user":{"name":"Artist"},"artwork":{"480x480":"https://art.example/a.jpg"},"genre":"Rock",$flags}""").jsonObject.toAudiusTrack()
        val track = parse(""""is_streamable":true,"is_available":true,"is_stream_gated":false,"stream_conditions":null""")
        assertEquals(song, track)
        assertNull(parse(""""is_streamable":true,"is_stream_gated":true"""))
        assertNull(parse(""""is_streamable":false"""))
        assertNull(parse(""""is_streamable":true,"stream_conditions":{"purchase":true}"""))
        assertNull(parse(""""is_streamable":true""", duration = 3600))
    }

    @Test fun `fallback selections are verified by provider and cannot inject playback URLs`() = runBlocking {
        val catalog = object : MusicCatalogClient {
            override suspend fun search(query: String, limit: Int): List<TrackSearchResult> = error("must resolve by id")
            override suspend fun findById(id: String) = song
            override fun close() = Unit
        }
        val service = MusicService(catalog)
        val submitted = SongEntry(song.id, song.title, song.artist, previewUrl = "https://attacker.example/audio.mp3")
        assertEquals(song.previewUrl, service.resolveEntry(submitted)?.previewUrl)
        assertNull(service.resolveEntry(submitted.copy(title = "Different Song")))
    }

    @Test fun `Deezer accepts its own secure preview URLs only`() {
        fun parse(url: String) = Json.parseToJsonElement("""{"id":42,"title":"Song","artist":{"name":"Artist"},"album":{"title":"Album"},"preview":"$url"}""").jsonObject.toDeezerTrack()
        assertEquals("deezer:42", parse("https://cdns-preview-a.dzcdn.net/stream.mp3")?.id)
        assertNull(parse("https://dzcdn.net.attacker.example/stream.mp3"))
        assertNull(parse("http://cdns-preview-a.dzcdn.net/stream.mp3"))
    }
}
