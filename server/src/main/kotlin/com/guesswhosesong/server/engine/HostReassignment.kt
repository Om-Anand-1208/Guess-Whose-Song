package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.Player

/**
 * Deterministic host reassignment:
 * promotes the earliest-joined, still-connected, non-kicked player.
 * This logic is isolated so it can be reused whenever a host disconnects.
 */
object HostReassignment {

    /**
     * Find the next host from [players], excluding [currentHostId].
     * Returns null if no eligible player exists (room should end).
     */
    fun findNextHost(players: List<Player>, currentHostId: String): Player? {
        return players
            .filter { it.id != currentHostId && it.connected && !it.isComputer }
            .minByOrNull { it.joinedAt }
    }

    /**
     * Apply the host change to [players], returning the updated list.
     * The old host loses host status; the new host gains it.
     */
    fun applyHostChange(players: List<Player>, oldHostId: String, newHostId: String): List<Player> {
        return players.map { player ->
            when (player.id) {
                oldHostId -> player.copy(isHost = false)
                newHostId -> player.copy(isHost = true)
                else -> player
            }
        }
    }
}
