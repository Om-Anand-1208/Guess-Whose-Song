package com.guesswhosesong.server.deezer

import com.guesswhosesong.shared.models.TrackSearchResult
import com.guesswhosesong.server.music.MusicCatalogClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val deezerJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@Serializable
private data class DeezerSearchResponse(
    val data: List<DeezerRawTrack> = emptyList()
)

@Serializable
private data class DeezerRawTrack(
    val id: Long? = null,
    val title: String? = null,
    @SerialName("preview") val previewUrl: String? = null,
    val artist: DeezerArtist? = null,
    val album: DeezerAlbum? = null
)

@Serializable
private data class DeezerArtist(
    val name: String? = null
)

@Serializable
private data class DeezerAlbum(
    @SerialName("cover_small") val coverSmall: String? = null,
    @SerialName("cover_medium") val coverMedium: String? = null,
    @SerialName("cover_big") val coverBig: String? = null,
    @SerialName("cover_xl") val coverXl: String? = null
)

class DeezerClient : MusicCatalogClient {
    private val logger = LoggerFactory.getLogger(DeezerClient::class.java)

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 10_000
        }
    }

    /**
     * Search Deezer for tracks matching [query].
     * Returns an empty list if the search fails or network is unavailable.
     */
    override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
        return try {
            val responseText: String = httpClient.get("https://api.deezer.com/search") {
                parameter("q", query)
                parameter("limit", limit)
                header("User-Agent", "Mozilla/5.0")
            }.bodyAsText()

            val response = deezerJson.decodeFromString<DeezerSearchResponse>(responseText)
            response.data
                .filter { !it.previewUrl.isNullOrBlank() && !it.title.isNullOrBlank() }
                .map { raw ->
                    TrackSearchResult(
                        id = (raw.id ?: 0L).toString(),
                        title = raw.title ?: "",
                        artist = raw.artist?.name ?: "Unknown Artist",
                        albumArtUrl = listOfNotNull(
                            raw.album?.coverXl,
                            raw.album?.coverBig,
                            raw.album?.coverMedium,
                            raw.album?.coverSmall
                        ).firstOrNull { it.isNotBlank() } ?: "",
                        previewUrl = raw.previewUrl ?: ""
                    )
                }
        } catch (e: Exception) {
            logger.error("Deezer search failed: ${e.message?.take(120)}")
            emptyList()
        }
    }

    override fun close() = httpClient.close()
}
