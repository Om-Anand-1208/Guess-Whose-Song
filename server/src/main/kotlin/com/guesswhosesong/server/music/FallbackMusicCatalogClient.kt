package com.guesswhosesong.server.music

import com.guesswhosesong.shared.models.TrackSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import org.slf4j.LoggerFactory
import java.net.URI

/** Prefer mainstream catalogs, while trying alternatives on failure or no matches. */
internal class FallbackMusicCatalogClient(
    private val catalogs: List<MusicCatalogClient>
) : MusicCatalogClient {
    override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
        var failure: MusicCatalogUnavailableException? = null
        var hadSuccessfulResponse = false
        for (catalog in catalogs) {
            try {
                val results = catalog.search(query, limit)
                hadSuccessfulResponse = true
                if (results.isNotEmpty()) return results
            } catch (e: MusicCatalogUnavailableException) {
                failure = e
            }
        }
        if (hadSuccessfulResponse) return emptyList()
        throw failure ?: MusicCatalogUnavailableException()
    }

    override suspend fun findById(id: String): TrackSearchResult? {
        val catalog = when {
            id.startsWith("deezer:") -> catalogs.filterIsInstance<DeezerCatalogClient>().firstOrNull()
            id.startsWith("audius:") -> catalogs.filterIsInstance<AudiusCatalogClient>().firstOrNull()
            else -> null
        }
        return catalog?.findById(id)
    }

    override fun close() = catalogs.forEach { it.close() }
}

private val catalogJson = Json { ignoreUnknownKeys = true }
private fun JsonObject.text(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.flag(name: String): Boolean? = (this[name] as? JsonPrimitive)?.booleanOrNull
private fun JsonObject.obj(name: String): JsonObject? = this[name] as? JsonObject

/** Shared transport for the public read-only catalog APIs. */
internal abstract class PublicCatalogClient : MusicCatalogClient {
    private val logger = LoggerFactory.getLogger(PublicCatalogClient::class.java)
    private val client = HttpClient(CIO) { engine { requestTimeout = 10_000 } }

    protected suspend fun get(url: String, parameters: Map<String, String> = emptyMap()): JsonObject {
        try {
            val response = client.get(url) { parameters.forEach { (key, value) -> parameter(key, value) } }
            if (response.status.value !in 200..299) {
                logger.warn("Catalog {} returned HTTP {}", URI(url).host, response.status.value)
                throw MusicCatalogUnavailableException()
            }
            return catalogJson.parseToJsonElement(response.bodyAsText()) as? JsonObject
                ?: throw MusicCatalogUnavailableException()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: MusicCatalogUnavailableException) {
            throw failure
        } catch (failure: Exception) {
            logger.warn("Catalog {} request failed: {}", URI(url).host, failure.javaClass.simpleName)
            throw MusicCatalogUnavailableException(failure)
        }
    }

    override fun close() = client.close()
}

internal class DeezerCatalogClient : PublicCatalogClient() {
    override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
        val result = get("https://api.deezer.com/search/track", mapOf("q" to query, "limit" to limit.coerceIn(1, 100).toString()))
        if (result["error"] != null) throw MusicCatalogUnavailableException()
        val tracks = result["data"] as? JsonArray ?: throw MusicCatalogUnavailableException()
        return tracks.mapNotNull { (it as? JsonObject)?.toDeezerTrack() }
    }

    override suspend fun findById(id: String): TrackSearchResult? {
        val rawId = id.removePrefix("deezer:")
        if (!id.startsWith("deezer:") || !rawId.matches(Regex("[0-9]{1,20}"))) return null
        val result = get("https://api.deezer.com/track/$rawId")
        if (result["error"] != null) return null
        return result.toDeezerTrack()
    }
}

internal fun JsonObject.toDeezerTrack(): TrackSearchResult? {
    val id = text("id")?.takeIf { it.matches(Regex("[0-9]{1,20}")) } ?: return null
    val title = text("title")?.takeIf { it.isNotBlank() && it.length <= 200 && it.none(Char::isISOControl) } ?: return null
    val artist = obj("artist")?.text("name")?.takeIf { it.isNotBlank() && it.length <= 120 && it.none(Char::isISOControl) } ?: return null
    val preview = text("preview")?.takeIf { url ->
        runCatching { URI(url) }.getOrNull()?.let { uri ->
            uri.scheme == "https" && (uri.host == "dzcdn.net" || uri.host?.endsWith(".dzcdn.net") == true)
        } == true
    } ?: return null
    val album = obj("album")
    return TrackSearchResult("deezer:$id", title, artist, album?.text("cover_big").orEmpty(), preview,
        albumTitle = album?.text("title").orEmpty())
}

internal class AudiusCatalogClient : PublicCatalogClient() {
    override suspend fun search(query: String, limit: Int): List<TrackSearchResult> {
        val result = get("https://api.audius.co/v1/tracks/search", mapOf("query" to query, "limit" to (limit.coerceIn(1, 100) * 3).coerceAtMost(100).toString()))
        val tracks = result["data"] as? JsonArray ?: throw MusicCatalogUnavailableException()
        return tracks.mapNotNull { (it as? JsonObject)?.toAudiusTrack() }.take(limit)
    }

    override suspend fun findById(id: String): TrackSearchResult? {
        val rawId = id.removePrefix("audius:")
        if (!id.startsWith("audius:") || !rawId.matches(Regex("[A-Za-z0-9]{1,64}"))) return null
        val result = get("https://api.audius.co/v1/tracks/$rawId")
        return result.obj("data")?.toAudiusTrack()
    }
}

internal fun JsonObject.toAudiusTrack(): TrackSearchResult? {
    val id = text("id")?.takeIf { it.matches(Regex("[A-Za-z0-9]{1,64}")) } ?: return null
    val title = text("title")?.takeIf { it.isNotBlank() && it.length <= 200 && it.none(Char::isISOControl) } ?: return null
    val uploader = obj("user")?.text("name")?.takeIf { it.isNotBlank() && it.length <= 110 && it.none(Char::isISOControl) } ?: return null
    val duration = (this["duration"] as? JsonPrimitive)?.longOrNull ?: return null
    if (duration !in 1..600) return null // Exclude DJ sets and long compilations.
    if (flag("is_streamable") != true || flag("is_available") == false || flag("is_stream_gated") == true ||
        flag("is_delete") == true || flag("is_unlisted") == true ||
        (this["stream_conditions"] != null && this["stream_conditions"] != JsonNull)) return null
    val artwork = obj("artwork")
    return TrackSearchResult(
        id = "audius:$id", title = title, artist = "$uploader (Audius)",
        albumArtUrl = artwork?.text("480x480") ?: artwork?.text("150x150").orEmpty(),
        previewUrl = "https://api.audius.co/v1/tracks/$id/stream",
        genreName = text("genre").orEmpty()
    )
}
