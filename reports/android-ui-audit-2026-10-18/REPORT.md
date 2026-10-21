# Android two-emulator visual audit — 18 October 2026

## Setup and result

I used `Android_API36_Slim` (`emulator-5556`, light) and `Android_API36_Slim_Copy` (`emulator-5558`, dark), both at **852 × 1844**. The local game server responded, and both apps created/joined rooms, selected suggested songs with artwork, locked picks, entered timed rounds, revealed submitters, and displayed final scores. I reversed host and guest roles to capture the host-only game settings in both themes.

The screenshots below are side-by-side comparisons. Full-resolution captures are in [`screenshots/`](screenshots/). This was an observation pass; no app code was changed.

## Follow-up fixes and verification

The Android app was updated after this observation pass. I built the debug APK and installed and relaunched it on both AVDs. I then ran a two-player, two-round game with the light and dark themes.

| Original finding | Change | Verification |
| --- | --- | --- |
| Dark Spotify Connect text | Use a lighter green label in dark mode. | Readable in the [dark selected-song screen](after/gws-dark-selected-final.png); compare [light](after/gws-light-selected-final.png). |
| Long titles overlap `FULL` | Reserve space between the song title and its status. | The “I Remember Everything…” row now stops before `FULL` in [light](after/gws-light-selected-final.png) and [dark](after/gws-dark-selected-final.png). |
| Equal scores get unequal ranks | Show ordinal ranks (`1st`, `2nd`, `3rd`, etc.) and give tied scores the same rank. Remove the trophy, medal, and Play Again music-note emojis. | Both players finished on 1 point and show `1st` in [light](after/light-results.png) and [dark](after/dark-results.png). |
| Dark lobby artwork loses outlines | Keep the same artwork in both themes and add a light outline behind it in dark mode. | Characters and instruments are visible in the [dark lobby](after/gws-dark-lobby-final.png); compare the [light lobby](after/light-lobby.png). |
| `ROUND 1 OF ?` during transition | Show `ROUND 1` until the total arrives, then show `ROUND 1 OF 2`. The compact round counter follows the same rule. | The [light](after/gws-light-round-final.png) and [dark](after/gws-dark-round-final.png) round screens show the known total; source review confirms the `?` fallback was removed. |
| Dark player-limit slider is subdued | Use a brighter yellow inactive track and contrasting tick colors. | Visible in the [dark settings sheet](after/gws-dark-settings-final.png); compare [light](after/light-settings.png). |
| Timed-out vote says “Not quite” | Show `Did not vote` and `No vote` when the guessed player ID is blank. Incorrect submitted guesses still say `Not quite`. | Source logic checked; the five-second reveal was not recaptured in the follow-up screenshots. |

The earlier emulator exit and room recovery observation was not reproduced during the two completed follow-up games. Its cause remains undetermined.

## Findings for review

| Priority | Finding | Evidence |
| --- | --- | --- |
| Medium | **Dark Spotify Connect text is hard to read.** The dark-green “Connect” label blends into the dark-green card, while the light version reads clearly. This is visible in both the initial and selected song states. | [Add songs](pairs/add-songs.png) |
| Medium | **Long song titles overlap the right-hand status.** Once the pick list is full, “I Remember Everything (feat. Kacey Musgrav...)” runs into the `FULL` label instead of leaving a clear gap. This happens in both themes. | [Selected song](pairs/selected-song.png) |
| Medium | **Equal scores receive unequal medals.** The final screen shows both players at `1 pts`, but one gets gold/first and the other silver/second. If ties are allowed, the rank presentation is misleading. | [Final scores](pairs/final-scores.png) |
| Low | **The dark lobby artwork loses some outlines and limbs.** The same four-character image is used in both themes, as intended, but its black linework blends into the dark background. Instruments and faces remain visible. | [Lobby](pairs/lobby.png) |
| Low | **The game transition briefly shows an unknown total.** Both themes display `ROUND 1 OF ?` on “Get ready” before the actual `1 OF 2` appears. | [Get ready](pairs/get-ready.png) |
| Low | **The dark player-limit slider's yellow half is subdued.** The yellow track and dots have less separation from the dark sheet than in light mode. The slider remains visible, so this is a polish issue. | [Game settings](pairs/game-settings.png) |
| Copy / flow | **A timed-out vote is described as “Not quite.”** In the captured reveal, the rows say `Voted (no vote)` and `Not quite`. That wording can read as an incorrect guess even when no choice was made. | [Reveal](pairs/reveal.png) |

## Screen gallery

| Screen or state | Side-by-side capture |
| --- | --- |
| Home | [Light and dark](pairs/home.png) |
| Player settings popup | [Light and dark](pairs/player-settings.png) |
| Two-player lobby | [Light and dark](pairs/lobby.png) |
| Host game settings sheet | [Light and dark](pairs/game-settings.png) |
| Add songs, no pick | [Light and dark](pairs/add-songs.png) |
| Add songs, one pick | [Light and dark](pairs/selected-song.png) |
| One player ready | [Light ready, dark still choosing](pairs/one-player-ready.png) |
| Get ready transition | [Light and dark](pairs/get-ready.png) |
| Listening / preview | [Light and dark](pairs/listening.png) |
| Vote, including decoy | [Light and dark](pairs/vote.png) |
| Reveal | [Light and dark](pairs/reveal.png) |
| Final scores | [Light and dark](pairs/final-scores.png) |

## Checks that looked consistent

- Home tagline wording and placement match across themes. The home illustrations are intentionally day/night variants.
- The lobby uses the same four-character artwork in both themes. Player rows use colored initials, including on vote, reveal, and final-score screens; no old player-character avatars appeared in the captured flow.
- The vote option circles, including `Nobody / Decoy`, align with the other choices in both themes.
- Player settings opens as a popup; the appearance control is absent. The lobby edit sheet contains only game settings, and its Save changes button stays beside the heading.
- Suggested song artwork populated on both AVDs. The room code and player counts matched across the shared sessions.

## Scope and limits

- I tested manual song picks through the core two-player flow. Spotify and Google authentication, Spotify search/sync, recently played mode, invite sharing, computer players, and physical-device audio were not tested.
- One dark AVD process exited during the first timed game. I restarted it and completed the later paired captures. Relaunching the app after that emulator restart opened Home instead of restoring the room. I did not determine whether the process exit or room recovery reflects an app issue; the later two-emulator game completed with both AVDs connected.
- Both AVDs and the local server were still running at the end of the audit. A `405` response from `GET /rooms` confirmed the local HTTP server was reachable; that endpoint expects a different method.
