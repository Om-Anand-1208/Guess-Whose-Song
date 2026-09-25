package com.guesswhosesong.server.engine

import com.guesswhosesong.shared.models.*

object ScoreEngine {

    const val DECOY_ID = GameConstants.DECOY_ID
    const val DECOY_NAME = GameConstants.DECOY_NAME

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
        val isDecoySong = songEntry.submitterId == DECOY_ID

        // 1. Evaluate individual votes
        val voteResults = players.map { voter ->
            val guessedId = votes[voter.id] ?: ""
            val guessedPlayerName = when (guessedId) {
                DECOY_ID -> DECOY_NAME
                else -> playerById[guessedId]?.displayName ?: "(no vote)"
            }

            val isSubmitter = (voter.id == songEntry.submitterId)
            // Submitter can never score by voting on their own song
            val isCorrect = if (isSubmitter) {
                false
            } else {
                guessedId == songEntry.submitterId
            }

            VoteResult(
                voterId = voter.id,
                voterName = voter.displayName,
                guessedPlayerId = guessedId,
                guessedPlayerName = guessedPlayerName,
                correct = isCorrect
            )
        }

        val scoreDeltas = mutableListOf<ScoreDelta>()

        // 2. Award 1 point to each correct guesser (who isn't the submitter)
        voteResults
            .filter { it.correct }
            .forEach { result ->
                val voter = playerById[result.voterId] ?: return@forEach
                scoreDeltas.add(
                    ScoreDelta(
                        playerId = voter.id,
                        playerName = voter.displayName,
                        delta = 1,
                        newTotal = voter.score + 1
                    )
                )
            }

        // 3. Submitter bonus: 1 point if it's a real player's song and NO ONE else guessed them!
        if (!isDecoySong) {
            val submitterPlayer = playerById[songEntry.submitterId]
            if (submitterPlayer != null) {
                val otherPlayers = players.filter { it.id != songEntry.submitterId }
                val othersGuessedSubmitter = otherPlayers.count { votes[it.id] == songEntry.submitterId }
                if (othersGuessedSubmitter == 0) {
                    val existingDelta = scoreDeltas.find { it.playerId == submitterPlayer.id }
                    if (existingDelta != null) {
                        scoreDeltas.remove(existingDelta)
                        scoreDeltas.add(
                            ScoreDelta(
                                playerId = submitterPlayer.id,
                                playerName = submitterPlayer.displayName,
                                delta = existingDelta.delta + 1,
                                newTotal = submitterPlayer.score + existingDelta.delta + 1
                            )
                        )
                    } else {
                        scoreDeltas.add(
                            ScoreDelta(
                                playerId = submitterPlayer.id,
                                playerName = submitterPlayer.displayName,
                                delta = 1,
                                newTotal = submitterPlayer.score + 1
                            )
                        )
                    }
                }
            }
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
