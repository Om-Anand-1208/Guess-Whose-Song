package com.guesswhosesong.server.itunes

import com.guesswhosesong.shared.models.TrackSearchResult
import com.guesswhosesong.server.music.MusicCatalogClient
import com.guesswhosesong.server.music.MusicCatalogUnavailableException
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val primaryGenreName: String? = null
)

internal fun highResolutionArtworkUrl(url: String): String =
    url.replace(Regex("/\\d+x\\d+bb\\.(jpg|png)$", RegexOption.IGNORE_CASE), "/600x600bb.$1")

class ItunesClient : MusicCatalogClient {
    private val logger = LoggerFactory.getLogger(ItunesClient::class.java)

    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 10_000
        }
    }

    /** Search iTunes, keeping genuine zero-match results separate from catalog failures. */
    override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
        return try {
            searchItunesWithRetry(
                request = {
                    val response = httpClient.get("https://itunes.apple.com/search") {
                        parameter("term", query)
                        parameter("media", "music")
                        parameter("limit", limit)
                        header("User-Agent", "Mozilla/5.0")
                    }
                    ItunesHttpResponse(response.status.value, response.bodyAsText())
                },
                onRetry = { attempt, status, bodyLength ->
                    logger.warn("iTunes search attempt {} failed: HTTP {}, {} bytes", attempt, status, bodyLength)
                }
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.error("iTunes search unavailable: {}", failure.message)
            throw MusicCatalogUnavailableException(failure)
        }
    }

    override fun close() = httpClient.close()
}

internal data class ItunesHttpResponse(val status: Int, val body: String)

private class ItunesResponseException(status: Int, bodyLength: Int) :
    IllegalStateException("HTTP $status, $bodyLength bytes")

internal suspend fun searchItunesWithRetry(
    request: suspend () -> ItunesHttpResponse,
    onRetry: (attempt: Int, status: Int?, bodyLength: Int?) -> Unit = { _, _, _ -> }
): List<TrackSearchResult> {
    repeat(3) { index ->
        val attempt = index + 1
        try {
            val response = request()
            val retryableStatus = response.status == HttpStatusCode.TooManyRequests.value || response.status in 500..599
            val emptyBody = response.body.isBlank()
            if (response.status !in 200..299 || emptyBody) {
                val failure = ItunesResponseException(response.status, response.body.length)
                if ((retryableStatus || (response.status in 200..299 && emptyBody)) && attempt < 3) {
                    onRetry(attempt, response.status, response.body.length)
                    delay(300L * attempt)
                    return@repeat
                }
                throw failure
            }

            val parsed = itunesJson.decodeFromString<ItunesSearchResponse>(response.body)
            return parsed.results
                .filter { !it.previewUrl.isNullOrBlank() && !it.trackName.isNullOrBlank() }
                .map { raw ->
                    TrackSearchResult(
                        id = (raw.trackId ?: raw.trackName.hashCode().toLong()).toString(),
                        title = raw.trackName ?: "",
                        artist = raw.artistName ?: "Unknown Artist",
                        albumArtUrl = raw.artworkUrl100?.let(::highResolutionArtworkUrl).orEmpty(),
                        previewUrl = raw.previewUrl ?: "",
                        genreName = raw.primaryGenreName.orEmpty(),
                        albumTitle = raw.collectionName.orEmpty()
                    )
                }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (failure is ItunesResponseException) throw failure
            if (attempt == 3) throw failure
            onRetry(attempt, null, null)
            delay(300L * attempt)
        }
    }
    error("iTunes retry loop finished without a response")
}
