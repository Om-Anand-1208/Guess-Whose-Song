package com.guesswhosesong.server.security

import com.guesswhosesong.shared.models.RoomSettings
import com.guesswhosesong.shared.models.SongEntry
import com.guesswhosesong.shared.models.SpotifySongId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class InputValidationTest {
    @Test
    fun `chat messages must contain bounded visible text`() {
        assertTrue(InputValidation.chatMessage("Hello room"))
        assertTrue(InputValidation.chatMessage("a".repeat(280)))
        assertFalse(InputValidation.chatMessage("   "))
        assertFalse(InputValidation.chatMessage("a".repeat(281)))
        assertFalse(InputValidation.chatMessage("hello\nthere"))
    }

    @Test
    fun `display names and room codes stay bounded`() {
        assertTrue(InputValidation.displayName("Alice"))
        assertFalse(InputValidation.displayName("\n"))
        assertTrue(InputValidation.joinCode("ABC234"))
        assertFalse(InputValidation.joinCode("../../"))
    }

    @Test
    fun `settings accept only supported values`() {
        assertTrue(InputValidation.settings(RoomSettings(playerLimit = 10, roundCount = 30, votingTimerSeconds = 30)))
        assertFalse(InputValidation.settings(RoomSettings(playerLimit = 1, votingTimerSeconds = 5)))
        assertFalse(InputValidation.settings(RoomSettings(roundCount = 1)))
        assertFalse(InputValidation.settings(RoomSettings(roundCount = 31)))
        assertFalse(InputValidation.settings(RoomSettings(playerLimit = 11)))
    }

    @Test
    fun `song payload rejects control characters and unsafe urls`() {
        val valid = SongEntry("123", "Song", "Artist", previewUrl = "https://example.com/a.mp3")
        assertTrue(InputValidation.song(valid))
        assertFalse(InputValidation.song(valid.copy(title = "Song\nInjected")))
        assertFalse(InputValidation.song(valid.copy(previewUrl = "file:///etc/passwd")))
    }

    @Test
    fun `spotify suggestion IDs remain valid for ordinary song titles`() {
        val title = "Blue Hours (Live)"
        val artist = "The Artist"
        val id = SpotifySongId.from(title, artist)
        assertTrue(InputValidation.song(SongEntry(id, title, artist, previewUrl = "")))
        assertFalse(InputValidation.song(SongEntry("${title}_${artist}", title, artist, previewUrl = "")))
    }

    @Test
    fun `playlist IDs use Spotify's fixed length alphabet`() {
        assertTrue(InputValidation.playlistId("1234567890123456789012"))
        assertFalse(InputValidation.playlistId("short"))
    }
}
