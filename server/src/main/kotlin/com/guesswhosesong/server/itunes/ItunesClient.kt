package com.guesswhosesong.server.itunes

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

private val itunesJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
private data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesRawTrack> = emptyList()
)

@Serializable
private data class ItunesRawTrack(
    val trackId: Long = 0L,
    val trackName: String = "",
    val artistName: String = "",
    val artworkUrl100: String = "",
    val previewUrl: String = ""
)

class ItunesClient {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(itunesJson)
        }
        engine {
            requestTimeout = 10_000
        }
    }

    /**
     * Search iTunes for tracks matching [query].
     * Returns an empty list if the search fails or network is unavailable.
     */
    suspend fun search(query: String, limit: Int = 10): List<TrackSearchResult> {
        return try {
            val response: ItunesSearchResponse = httpClient.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("limit", limit)
            }.body()
            response.results
                .filter { it.previewUrl.isNotBlank() }
                .map { raw ->
                    TrackSearchResult(
                        id = raw.trackId.toString(),
                        title = raw.trackName,
                        artist = raw.artistName,
                        albumArtUrl = raw.artworkUrl100,
                        previewUrl = raw.previewUrl
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun close() = httpClient.close()
}

