package com.guesswhosesong.server.music

import com.guesswhosesong.shared.models.TrackSearchResult

interface MusicCatalogClient {
    suspend fun search(query: String, limit: Int = 10): List<TrackSearchResult>
    fun close()
}
