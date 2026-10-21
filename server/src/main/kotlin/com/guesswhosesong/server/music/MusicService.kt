package com.guesswhosesong.server.music

import com.guesswhosesong.server.itunes.ItunesClient
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.SpotifySuggestion
import com.guesswhosesong.shared.models.TrackSearchResult

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class ResolvedSong(val song: SongEntry, val genreName: String)

/** Uses iTunes as the catalog and source for track previews and artwork. */
class MusicService(
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

            // A catalog failure throws, so only real search responses enter the cache.
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
            exactArtistResults
                .asSequence()
                .filterNot { trackKey(it.title, it.artist) in existingTracks }
                .forEach { candidates += it }
            if (candidates.distinctBy { trackKey(it.title, it.artist) }.size >= limit) break
        }

        candidates.distinctBy { trackKey(it.title, it.artist) }.shuffled().take(limit)
    }

    /** Find one different-artist, playable candidate for each submitted song's iTunes genre. */
    internal suspend fun getGenreMatchedTracks(
        submittedSongs: List<SongEntry>,
        seedGenres: List<String>
    ): List<TrackSearchResult> {
        val genres = seedGenres.map(String::trim).filter(String::isNotBlank)
        if (genres.isEmpty()) return emptyList()

        val usedTrackIds = submittedSongs.mapTo(mutableSetOf()) { it.songId }
        val usedTrackKeys = submittedSongs.mapTo(mutableSetOf()) { trackKey(it.title, it.artist) }
        val submittedArtists = submittedSongs.mapTo(mutableSetOf()) { normalize(it.artist) }
        val candidatesByGenre = mutableMapOf<String, List<TrackSearchResult>>()

        for (genre in genres.distinctBy(::normalize)) {
            val genreKey = normalize(genre)
            val searchTerm = genre.substringBefore('/').trim()
            fun List<TrackSearchResult>.matchingGenre() = filter { track ->
                normalize(track.genreName) == genreKey &&
                    track.previewUrl.isNotBlank() &&
                    normalize(track.artist) !in submittedArtists
            }
            var candidates = search(searchTerm, 100).matchingGenre()
            if (candidates.isEmpty() && searchTerm != genre) {
                candidates = search(genre, 100).matchingGenre()
            }
            candidatesByGenre[genreKey] = candidates.shuffled()
        }

        val selected = mutableListOf<TrackSearchResult>()
        for (genre in genres.shuffled()) {
            val next = candidatesByGenre[normalize(genre)].orEmpty().firstOrNull { track ->
                track.id !in usedTrackIds && trackKey(track.title, track.artist) !in usedTrackKeys
            } ?: continue
            selected += next
            usedTrackIds += next.id
            usedTrackKeys += trackKey(next.title, next.artist)
        }
        return selected
    }

    /**
     * Resolve a [SongEntry] that already has title+artist, filling in the previewUrl.
     * Returns null if resolution fails (caller should reject the song).
     */
    suspend fun resolveEntry(entry: SongEntry): SongEntry? = resolveEntryWithGenre(entry)?.song

    /** Automatic Spotify modes require an exact title, artist, and album match in iTunes. */
    internal suspend fun resolveSpotifyTrackWithGenre(track: SpotifySuggestion): ResolvedSong? =
        externalRequestLimit.withPermit {
            if (track.title.isBlank() || track.artist.isBlank() || track.albumTitle.isBlank()) {
                return@withPermit null
            }
            val results = try {
                itunesClient.search("${track.title} ${track.artist}", 50)
            } catch (_: MusicCatalogUnavailableException) {
                return@withPermit null
            }
            val match = results.firstOrNull { result ->
                result.previewUrl.isNotBlank() &&
                    sameMetadata(result.title, track.title) &&
                    sameMetadata(result.artist, track.artist) &&
                    sameMetadata(result.albumTitle, track.albumTitle)
            } ?: return@withPermit null

            ResolvedSong(
                song = SongEntry(
                    songId = match.id,
                    title = match.title,
                    artist = match.artist,
                    albumArtUrl = track.albumArtUrl.ifBlank { match.albumArtUrl },
                    previewUrl = match.previewUrl
                ),
                genreName = match.genreName
            )
        }

    internal suspend fun resolveEntryWithGenre(entry: SongEntry): ResolvedSong? = externalRequestLimit.withPermit {
        val query = "${entry.title.trim()} ${entry.artist.trim()}".trim()

        val results = try {
            itunesClient.search(query, 10)
        } catch (unavailable: MusicCatalogUnavailableException) {
            return@withPermit null
        }
        val bestMatch = findBestMatch(results, query) ?: return@withPermit null

        ResolvedSong(
            song = entry.copy(
                songId = bestMatch.id,
                title = bestMatch.title,
                artist = bestMatch.artist,
                albumArtUrl = entry.albumArtUrl.takeIf { it.isNotBlank() }
                    ?: bestMatch.albumArtUrl,
                previewUrl = bestMatch.previewUrl
            ),
            genreName = bestMatch.genreName
        )
    }

    private suspend fun searchCatalog(query: String, limit: Int): List<TrackSearchResult> =
        itunesClient.search(query, limit)

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

    private fun sameMetadata(first: String, second: String): Boolean =
        first.isNotBlank() && second.isNotBlank() &&
            normalize(first).replace(" ", "") == normalize(second).replace(" ", "")

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
        itunesClient.close()
    }
}
