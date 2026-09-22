package com.guesswhosesong.server.music

import com.guesswhosesong.server.deezer.DeezerClient
import com.guesswhosesong.server.itunes.ItunesClient
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.TrackSearchResult

/**
 * Service that unifies Deezer and iTunes API lookups.
 * Priority: Deezer -> fallback to iTunes if no results.
 */
class MusicService(
    private val deezerClient: DeezerClient = DeezerClient(),
    private val itunesClient: ItunesClient = ItunesClient()
) {

    suspend fun search(query: String, limit: Int = 10): List<TrackSearchResult> {
        val deezerResults = deezerClient.search(query, limit)
        if (deezerResults.isNotEmpty()) {
            return deezerResults
        }
        return itunesClient.search(query, limit)
    }

    suspend fun getTopTracks(): List<TrackSearchResult> {
        // Fallback to iTunes for generic top tracks since Deezer top charts require specific region lookups or playlists
        return itunesClient.search("Hits 2024", 20)
    }

    /**
     * Resolve a [SongEntry] that already has title+artist, filling in the previewUrl.
     * Returns null if resolution fails (caller should reject the song).
     */
    suspend fun resolveEntry(entry: SongEntry): SongEntry? {
        val query = "${entry.title.trim()} ${entry.artist.trim()}".trim()
        
        var bestMatch = findBestMatch(deezerClient.search(query, 10), query)
        
        if (bestMatch == null) {
            bestMatch = findBestMatch(itunesClient.search(query, 10), query)
        }

        if (bestMatch == null) return null

        return entry.copy(
            songId = bestMatch.id,
            title = bestMatch.title,
            artist = bestMatch.artist,
            albumArtUrl = bestMatch.albumArtUrl,
            previewUrl = bestMatch.previewUrl
        )
    }

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
        s.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()

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
}

