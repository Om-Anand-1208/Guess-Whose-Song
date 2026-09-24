package com.guesswhosesong.server.itunes

import com.guesswhosesong.shared.models.TrackSearchResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val itunesJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Serializable
private data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesRawTrack> = emptyList()
)

@Serializable
private data class ItunesRawTrack(
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null
)

class ItunesClient {
    private val logger = LoggerFactory.getLogger(ItunesClient::class.java)

    private val httpClient = HttpClient(CIO) {
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
            val responseText: String = httpClient.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("limit", limit)
                header("User-Agent", "Mozilla/5.0")
            }.bodyAsText()

            val response = itunesJson.decodeFromString<ItunesSearchResponse>(responseText)
            response.results
                .filter { !it.previewUrl.isNullOrBlank() && !it.trackName.isNullOrBlank() }
                .map { raw ->
                    TrackSearchResult(
                        id = (raw.trackId ?: raw.trackName.hashCode().toLong()).toString(),
                        title = raw.trackName ?: "",
                        artist = raw.artistName ?: "Unknown Artist",
                        albumArtUrl = raw.artworkUrl100 ?: "",
                        previewUrl = raw.previewUrl ?: ""
                    )
                }
        } catch (e: Exception) {
            logger.error("iTunes search failed for query '$query': ${e.message}", e)
            emptyList()
        }
    }

    fun close() = httpClient.close()
}
