package com.guesswhosesong.server.spotify

import com.guesswhosesong.shared.models.SpotifyCategory
import com.guesswhosesong.shared.models.SpotifySuggestion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.CancellationException

private val spotifyJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Serializable
private data class SpotifyTopTracksResponse(
    val items: List<SpotifyTrackItem> = emptyList()
)

@Serializable
private data class SpotifyRecentResponse(
    val items: List<SpotifyPlayHistoryObject> = emptyList()
)

@Serializable
private data class SpotifyPlayHistoryObject(
    val track: SpotifyTrackItem = SpotifyTrackItem()
)

@Serializable
private data class SpotifyPlaylistTracksResponse(
    val items: List<SpotifyPlaylistTrackItem> = emptyList()
)

@Serializable
private data class SpotifyPlaylistTrackItem(
    val track: SpotifyTrackItem? = null
)

@Serializable
private data class SpotifyTrackItem(
    val id: String? = null,
    val name: String = "",
    val artists: List<SpotifyArtistItem> = emptyList(),
    val album: SpotifyAlbumItem = SpotifyAlbumItem()
)

@Serializable
private data class SpotifyArtistItem(val name: String = "")

@Serializable
private data class SpotifyAlbumItem(
    val images: List<SpotifyImageItem> = emptyList()
)

@Serializable
private data class SpotifyImageItem(val url: String = "")

private fun SpotifyTrackItem.toSuggestion(category: SpotifyCategory) = SpotifySuggestion(
    title = name,
    artist = artists.firstOrNull()?.name ?: "",
    albumArtUrl = album.images.firstOrNull()?.url ?: "",
    category = category,
    spotifyTrackId = id
)

/**
 * Makes calls to the Spotify Web API on behalf of a user using their access token.
 * All methods return empty lists on network/auth failure so the UI degrades gracefully.
 */
class SpotifyClient {

    class UnauthorizedException : Exception("Spotify authorization expired")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(spotifyJson) }
    }
    private val externalRequestLimit = Semaphore(4)

    suspend fun getTopTracks(accessToken: String, limit: Int = 50): List<SpotifySuggestion> = externalRequestLimit.withPermit {
        try {
            val response: SpotifyTopTracksResponse = httpClient.get("https://api.spotify.com/v1/me/top/tracks") {
                bearerAuth(accessToken)
                parameter("limit", limit)
                parameter("time_range", "medium_term")
            }.body()
            response.items.map { it.toSuggestion(SpotifyCategory.TOP_TRACKS) }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
            emptyList()
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getRecentlyPlayed(accessToken: String, limit: Int = 50): List<SpotifySuggestion> = externalRequestLimit.withPermit {
        try {
            val response: SpotifyRecentResponse = httpClient.get("https://api.spotify.com/v1/me/player/recently-played") {
                bearerAuth(accessToken)
                parameter("limit", limit)
            }.body()
            response.items.map { it.track.toSuggestion(SpotifyCategory.RECENTLY_PLAYED) }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
            emptyList()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { emptyList() }
    }

    suspend fun getPlaylistTracks(accessToken: String, playlistId: String): List<SpotifySuggestion> = externalRequestLimit.withPermit {
        try {
            val response: SpotifyPlaylistTracksResponse =
                httpClient.get("https://api.spotify.com/v1/playlists/$playlistId/tracks") {
                    bearerAuth(accessToken)
                    parameter("limit", 50)
                }.body()
            response.items.mapNotNull { it.track?.toSuggestion(SpotifyCategory.PLAYLIST) }
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) throw UnauthorizedException()
            emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun close() = httpClient.close()
}
