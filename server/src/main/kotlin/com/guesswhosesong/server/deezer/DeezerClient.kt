package com.guesswhosesong.server.deezer

import com.guesswhosesong.shared.models.TrackSearchResult
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val deezerJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class DeezerSearchResponse(
    val data: List<DeezerRawTrack> = emptyList()
)

@Serializable
private data class DeezerRawTrack(
    val id: Long = 0L,
    val title: String = "",
    @SerialName("preview") val previewUrl: String = "",
    val artist: DeezerArtist = DeezerArtist(),
    val album: DeezerAlbum = DeezerAlbum()
)

@Serializable
private data class DeezerArtist(
    val name: String = ""
)

@Serializable
private data class DeezerAlbum(
    @SerialName("cover_medium") val coverMedium: String = ""
)

class DeezerClient {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(deezerJson)
        }
        engine {
            requestTimeout = 10_000
        }
    }

    /**
     * Search Deezer for tracks matching [query].
     * Returns an empty list if the search fails or network is unavailable.
     */
    suspend fun search(query: String, limit: Int = 10): List<TrackSearchResult> {
        return try {
            val response: DeezerSearchResponse = httpClient.get("https://api.deezer.com/search/track") {
                parameter("q", query)
                parameter("limit", limit)
            }.body()
            response.data
                .filter { it.previewUrl.isNotBlank() }
                .map { raw ->
                    TrackSearchResult(
                        id = raw.id.toString(),
                        title = raw.title,
                        artist = raw.artist.name,
                        albumArtUrl = raw.album.coverMedium,
                        previewUrl = raw.previewUrl
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun close() = httpClient.close()
}

