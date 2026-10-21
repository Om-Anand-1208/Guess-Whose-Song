package com.guesswhosesong.server.itunes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlinx.coroutines.runBlocking

class ItunesClientTest {
    private val oneTrackResponse = """{"resultCount":1,"results":[{"trackId":123,"trackName":"Blue Monday","artistName":"New Order","collectionName":"Power, Corruption & Lies","previewUrl":"https://example.com/preview.mp3"}]}"""

    @Test
    fun `retries an empty iTunes response instead of reporting no matches`() = runBlocking {
        var calls = 0
        val results = searchItunesWithRetry(request = {
            calls++
            if (calls == 1) ItunesHttpResponse(200, "") else ItunesHttpResponse(200, oneTrackResponse)
        })

        assertEquals(2, calls)
        assertEquals("Blue Monday", results.single().title)
        assertEquals("Power, Corruption & Lies", results.single().albumTitle)
    }

    @Test
    fun `does not retry a permanent HTTP error`() {
        var calls = 0
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                searchItunesWithRetry(request = {
                    calls++
                    ItunesHttpResponse(403, "Forbidden")
                })
            }
        }
        assertEquals(1, calls)
    }

    @Test
    fun `a valid empty result remains a real no-match response`() = runBlocking {
        val results = searchItunesWithRetry(request = {
            ItunesHttpResponse(200, """{"resultCount":0,"results":[]}""")
        })

        assertEquals(emptyList<Any>(), results)
    }

    @Test
    fun `requests the 600px iTunes artwork variant`() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/example.jpg/600x600bb.jpg",
            highResolutionArtworkUrl("https://is1-ssl.mzstatic.com/image/thumb/Music/example.jpg/100x100bb.jpg")
        )
    }

    @Test
    fun `leaves unrecognized artwork paths unchanged`() {
        val url = "https://art.example/cover.jpg"
        assertEquals(url, highResolutionArtworkUrl(url))
    }
}
