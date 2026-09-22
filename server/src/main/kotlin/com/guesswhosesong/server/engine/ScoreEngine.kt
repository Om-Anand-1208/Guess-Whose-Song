package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.*

object ScoreEngine {

    /**
     * Compute vote results and score deltas for the current round.
     *
     * @param songEntry The round's song entry (includes submitterId)
     * @param votes Map of voterId -> guessedPlayerId
     * @param players Current player list (for display names)
     * @return Pair of (voteResults, scoreDeltas)
     */
    fun computeRoundResults(
        songEntry: SongEntry,
        votes: Map<String, String>,
        players: List<Player>
    ): Pair<List<VoteResult>, List<ScoreDelta>> {
        val playerById = players.associateBy { it.id }

        val voteResults = players.map { voter ->
            val guessedId = votes[voter.id] ?: ""
            val guessedPlayer = playerById[guessedId]
            VoteResult(
                voterId = voter.id,
                voterName = voter.displayName,
                guessedPlayerId = guessedId,
                guessedPlayerName = guessedPlayer?.displayName ?: "(no vote)",
                correct = guessedId == songEntry.submitterId
            )
        }

        val scoreDeltas = voteResults
            .filter { it.correct }
            .map { result ->
                val voter = playerById[result.voterId]!!
                ScoreDelta(
                    playerId = voter.id,
                    playerName = voter.displayName,
                    delta = 1,
                    newTotal = voter.score + 1
                )
            }

        return Pair(voteResults, scoreDeltas)
    }

    /**
     * Apply score deltas to the player list, returning an updated list.
     */
    fun applyDeltas(players: List<Player>, scoreDeltas: List<ScoreDelta>): List<Player> {
        val deltaById = scoreDeltas.associateBy { it.playerId }
        return players.map { player ->
            val delta = deltaById[player.id]
            if (delta != null) player.copy(score = player.score + delta.delta)
            else player
        }
    }
}
