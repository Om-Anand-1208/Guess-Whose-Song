package com.guesswhosesong.server.music

import com.guesswhosesong.server.deezer.DeezerClient
import com.guesswhosesong.server.itunes.ItunesClient
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Prefer Deezer for tracks and previews, with iTunes as the fallback catalog. */
class MusicService(
    private val deezerClient: MusicCatalogClient = DeezerClient(),
    private val itunesClient: MusicCatalogClient = ItunesClient()
) {

    // Simple thread-safe LRU cache using LinkedHashMap, bounded to 200 items.
    private val searchCache = java.util.Collections.synchronizedMap(
        object : java.util.LinkedHashMap<String, List<TrackSearchResult>>(200, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<TrackSearchResult>>?): Boolean {
                return size > 200
            }
        }
    )
    private val externalRequestLimit = Semaphore(8)

    suspend fun search(query: String, limit: Int = 10): List<TrackSearchResult> {
        return externalRequestLimit.withPermit {
            val cacheKey = "${query.trim().lowercase()}_$limit"
            searchCache[cacheKey]?.let { return@withPermit it }

            searchCatalog(query, limit).also { searchCache[cacheKey] = it }
        }
    }

    suspend fun getTopTracks(): List<TrackSearchResult> = externalRequestLimit.withPermit {
        searchCatalog("Pop Hits", 30).ifEmpty { searchCatalog("Top Songs", 30) }
    }

    /**
     * Find playable tracks by artists already represented in the pool. This keeps
     * computer picks and decoys in the room's existing musical context.
     */
    suspend fun getContextualTracks(
        poolSongs: List<SongEntry>,
        limit: Int
    ): List<TrackSearchResult> = externalRequestLimit.withPermit {
        if (limit <= 0) return@withPermit emptyList()
        if (poolSongs.isEmpty()) {
            return@withPermit searchCatalog("Pop Hits", maxOf(limit * 5, 30))
                .ifEmpty { searchCatalog("Top Songs", maxOf(limit * 5, 30)) }
                .take(limit)
        }

        val existingTracks = poolSongs.mapTo(mutableSetOf()) { trackKey(it.title, it.artist) }
        val artists = poolSongs.asSequence()
            .map { it.artist.trim() }
            .filter(String::isNotBlank)
            .groupBy(::normalize)
            .values
            .sortedByDescending { it.size }
            .take(6)
            .map { it.first() }

        val candidates = mutableListOf<TrackSearchResult>()
        for (artist in artists) {
            val artistKey = normalize(artist)
            val artistResults = searchCatalog(artist, maxOf(limit * 6, 20))
            val exactArtistResults = artistResults.filter { normalize(it.artist) == artistKey }
                .ifEmpty {
                    itunesClient.search(artist, maxOf(limit * 6, 20))
                        .filter { normalize(it.artist) == artistKey }
                }
            exactArtistResults
                .asSequence()
                .filterNot { trackKey(it.title, it.artist) in existingTracks }
                .forEach { candidates += it }
            if (candidates.distinctBy { trackKey(it.title, it.artist) }.size >= limit) break
        }

        candidates.distinctBy { trackKey(it.title, it.artist) }.shuffled().take(limit)
    }

    /**
     * Resolve a [SongEntry] that already has title+artist, filling in the previewUrl.
     * Returns null if resolution fails (caller should reject the song).
     */
    suspend fun resolveEntry(entry: SongEntry): SongEntry? = externalRequestLimit.withPermit {
        val query = "${entry.title.trim()} ${entry.artist.trim()}".trim()

        val deezerMatch = findBestMatch(deezerClient.search(query, 10), query)
        val itunesMatch = if (deezerMatch == null || deezerMatch.albumArtUrl.isBlank()) {
            findBestMatch(itunesClient.search(query, 10), query)
        } else null
        val bestMatch = deezerMatch ?: itunesMatch ?: return@withPermit null

        entry.copy(
            songId = bestMatch.id,
            title = bestMatch.title,
            artist = bestMatch.artist,
            albumArtUrl = entry.albumArtUrl.takeIf { it.isNotBlank() }
                ?: deezerMatch?.albumArtUrl?.takeIf { it.isNotBlank() }
                ?: itunesMatch?.albumArtUrl.orEmpty(),
            previewUrl = bestMatch.previewUrl
        )
    }

    private suspend fun searchCatalog(query: String, limit: Int): List<TrackSearchResult> {
        val deezerResults = deezerClient.search(query, limit)
        if (deezerResults.isEmpty()) return itunesClient.search(query, limit)
        if (deezerResults.none { it.albumArtUrl.isBlank() }) return deezerResults

        val itunesResults = itunesClient.search(query, limit.coerceAtLeast(deezerResults.size))
        return deezerResults.map { deezerTrack ->
            if (deezerTrack.albumArtUrl.isNotBlank()) {
                deezerTrack
            } else {
                val match = findBestMatch(itunesResults, "${deezerTrack.title} ${deezerTrack.artist}")
                deezerTrack.copy(albumArtUrl = match?.albumArtUrl?.takeIf { it.isNotBlank() }.orEmpty())
            }
        }
    }

    private fun trackKey(title: String, artist: String): String =
        "${normalize(title)}|${normalize(artist)}"

    private fun findBestMatch(results: List<TrackSearchResult>, query: String): TrackSearchResult? {
        if (results.isEmpty()) return null

        val best = results.minByOrNull { track ->
            levenshtein(
                normalize("${track.title} ${track.artist}"),
                normalize(query)
            )
        } ?: return null

        // Reject if the best match is too far from the query (heuristic: distance > 60% of query length)
        val distance = levenshtein(normalize("${best.title} ${best.artist}"), normalize(query))
        val threshold = (query.length * 0.6).toInt().coerceAtLeast(5)
        if (distance > threshold) return null

        return best
    }

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), "").trim()

    /** Standard iterative Levenshtein distance */
    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    fun close() {
        deezerClient.close()
        itunesClient.close()
    }
}
