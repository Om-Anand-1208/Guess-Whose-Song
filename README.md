# Guess Whose Song

A multiplayer party quiz app. Players submit songs anonymously. Everyone votes on who submitted each clip. Correct guesses earn points.

**[Download the Android APK](artifacts/GuessWhoseSong.apk)**

## Screenshots

The checked-in captures below show the Android flow in both themes.

| Lobby | Song selection | Voting | Results |
| --- | --- | --- | --- |
| ![Light lobby](docs/screenshots/light-lobby.png) | ![Light song selection](docs/screenshots/add-songs-light.png) | ![Dark round](docs/screenshots/dark-round.png) | ![Light results](docs/screenshots/light-results.png) |

Dark-theme references: [dark lobby](docs/screenshots/dark-lobby.png), [dark song selection](docs/screenshots/add-songs-dark.png), and [dark results](docs/screenshots/dark-results.png).

## System design

```mermaid
flowchart LR
    subgraph Android[Android client]
        UI[Jetpack Compose screens]
        Identity[Firebase identity manager]
        Poller[Authenticated REST and long polling]
        UI --> Identity
        UI --> Poller
    end

    Auth[Firebase Authentication]
    API[Ktor API on Render]
    Verify[Firebase Admin token verifier]
    Redis[(Render Redis)]
    Music[Music service]
    ITunes[iTunes Search API]
    Spotify[Spotify OAuth and Web API]
    Hosting[Firebase Hosting<br/>App Links file and browser fallback]
    Shared[shared Kotlin module<br/>models, actions, events]

    Identity -->|anonymous sign-in or Google link| Auth
    Poller -->|HTTPS with Firebase ID token| API
    API --> Verify
    Verify -->|verify signature, audience, expiry, UID| Auth
    API --> Redis
    API --> Music
    Music -->|search, previews, artwork| ITunes
    API --> Spotify
    API -. shared protocol .- Shared
    UI -. shared protocol .- Shared
    Invite[Room invite URL] --> Hosting
    Hosting -->|verified App Link when installed| Android
```

The Android APK is the only game client. Firebase Hosting does not run a browser version of the game: it serves Android App Links verification and a small fallback page for invite links opened on devices without the app. With the APK installed, Android verifies the hosting domain and opens `/join/{CODE}` directly in the app.

The Kotlin `shared/` module defines the wire contract used by both Android and the server: room and player models, client actions, server events, and JSON serialization. The server is authoritative. It validates actions and decides room membership, timers, song-pool eligibility, votes, scores, and round results; the client renders server state and submits user actions.

### Identity and request flow

1. On first launch, Android signs in anonymously with Firebase Authentication and obtains a Firebase ID token. Players may link a Google account later; a successful link keeps the same Firebase UID. If the Google account already belongs to a different UID, the app reports the collision rather than merging accounts.
2. The app sends the ID token as `Authorization: Bearer ...` on API calls. `FirebaseTokenVerifier` validates it with the Firebase Admin SDK and the project ID `guess-whose-song-om-anand-1208` before the backend uses the verified UID as the player ID.
3. The same Firebase project backs Android Authentication, server token verification, and the invite-link Hosting site. `app/google-services.json`, Render's `FIREBASE_PROJECT_ID`, and the Admin service-account credential must all belong to that project.

### Rooms and live updates

1. An authenticated player creates a room with `POST /rooms`; the server creates a short join code and makes the verified UID the host. Other players join with the code and their display name and avatar.
2. Android posts actions to `/rooms/{code}/actions`. It receives updates through `GET /rooms/{code}/events`, a 20-second long poll. Temporary network errors retry with exponential backoff; leaving, being kicked, or room end stops polling.
3. `RoomSession` runs the game state machine. Redis stores room snapshots, private runtime state, chat history, Spotify authorization state/tokens, and rate-limit counters. Room data expires after four hours of inactivity and is also deleted when a room ends.
4. Render runs one Ktor instance because live room ownership and event delivery are not distributed across instances. Horizontal scaling needs shared room coordination and publish/subscribe support first.

### Music and account connections

- In manual mode, players search the server's iTunes-backed catalog and submit songs. iTunes supplies playable preview URLs, artwork, and metadata. The server resolves tracks, applies room rules, and builds the final pool.
- Top Tracks, Recently Played, and playlist imports are optional Spotify features. The server starts a Spotify authorization flow with PKCE, keeps the one-time state and verifier in Redis, exchanges the callback code, and stores the user's Spotify access token in Redis. Spotify tokens stay on the server and are removed when they expire or the player disconnects.
- Spotify tracks are matched against iTunes by normalized title, artist, and album. Only a usable iTunes preview enters the round pool; duplicate or ambiguous tracks are removed. Spotify features return an unavailable response when their credentials are not configured.

### Hosting and deployment boundary

- Render runs the Dockerized Ktor API and a private Redis service. The API exposes `/health` for Render's health check and uses environment variables for Firebase, Redis, and optional Spotify credentials.
- Firebase Hosting at `guess-whose-song-game.web.app` serves `assetlinks.json` and the invite fallback page only. The fallback shows the room code and links to the Android APK; game state and gameplay requests go to the Render API.
- The Firebase Admin key is a deployment secret. Keep it, `google-services.json`, Spotify secrets, and Redis credentials out of source control and APK artifacts.

## Project Structure

```
MusicAppProject/
├── shared/   # Kotlin Multiplatform — DTOs shared between server and Android
├── server/   # Ktor backend (game engine, Redis, iTunes/Spotify/Firebase)
├── app/      # Android client (Jetpack Compose)
├── hosting/  # Android App Links verification, APK, and fallback page
├── docs/     # Checked-in screenshots and project documentation
└── artifacts/ # APK intended for manual download or GitHub Releases
```

### Why `shared/` matters

`shared/` is the protocol boundary. It prevents the Android client and server from disagreeing about JSON or game state.

- `models/Models.kt` defines `Room`, `Player`, `SongEntry`, `RoomSettings`, avatar data, score data, and song metadata.
- `models/Enums.kt` defines room states, round phases, game modes, voting timers, and round-count rules.
- `dto/ClientMessages.kt` defines actions such as `START_GAME`, `LOCK_SONG`, `CAST_VOTE`, `SKIP_PREVIEW`, `PLAY_AGAIN`, `END_ROOM`, and settings updates.
- `dto/ServerMessages.kt` defines snapshots and events such as `ROOM_JOINED`, `ROOM_UPDATED`, `ROUND_PREVIEW_STARTED`, `VOTING_STARTED`, `ROUND_REVEALED`, `GAME_RESULTS`, and `ROOM_ENDED`.
- `MessageSerializer.kt` keeps the discriminator format consistent across Android and JVM.

The server uses these same types for validation and serialization, so a protocol change is visible to all clients at compile time.

## Setup

### 1. Firebase
- Use the Firebase project `guess-whose-song-om-anand-1208` (project name: Guess Whose Song), or create your own project and update the project ID in `render.yaml` and `FirebaseTokenVerifier.kt`.
- Enable **Anonymous Authentication**
- Enable **Google** sign-in. Guests may link Google later; linking preserves the anonymous UID and a collision is rejected (accounts are never merged automatically).
- Register the Android app's debug and release SHA-1 fingerprints in Firebase Project settings so Google sign-in can validate the installed APK.
- After enabling Google, download the latest `google-services.json` for the Android app and replace `app/google-services.json`. The config must include the web OAuth client ID because Android reads it as `default_web_client_id` for the Google sign-in flow.
- For the server: create a Firebase Admin service-account key in this same project and provide it through the deployment secret `FIREBASE_SERVICE_ACCOUNT_JSON`, or mount it as a Render secret file and set `FIREBASE_SERVICE_ACCOUNT_PATH`. Never copy credentials into Docker or commit them.

### 2. Spotify (Optional — for Top Tracks and Recently Played modes)
- Create an app at https://developer.spotify.com/dashboard
- Set the redirect URI to your server URL + `/spotify/callback`
- Set env vars: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`

### 3. Redis
- Local: `redis-server` (default port 6379)
- Production: set `REDIS_URL` env var (including `rediss://` for TLS where required, e.g. Render Redis add-on URL)

### 4. Server

The production API is hosted at `https://guess-whose-song-server.onrender.com`.
Spotify's registered server callback must be
`https://guess-whose-song-server.onrender.com/spotify/callback`.

```bash
./gradlew :server:shadowJar
java -jar server/build/libs/guess-whose-song-server.jar
```

Run only one Ktor server on port 8080. If it is already running in another terminal or tmux pane, reuse it; stop that process before starting another instance, or the second start will fail with `Address already in use`. The ngrok tunnel can remain running while the server is restarted.

### 5. Android
- Open in Android Studio
- Set the `gwsApiBaseUrl` Gradle property to point to your server; it defaults to `https://guess-whose-song-server.onrender.com`
- Run on device or emulator

### 6. Room invite links

Room invites use verified Android App Links at `https://guess-whose-song-game.web.app/join/{CODE}`. On an Android device with the app installed, Android opens the invite directly in the app and joins that room. Firebase Hosting serves only the App Links verification file and a small fallback page for browsers or devices without the app; the fallback shows the room code and links to the Android APK.

The `guess-whose-song-om-anand-1208` Firebase project serves the Android App Links fallback, Android Authentication, and server ID-token verification. The Android configuration in `app/google-services.json` must belong to this project, and Render must use the same project ID and a service-account key created in this project.

The hosting files are in `hosting/`. To publish the fallback page, configure `.firebaserc` for the `guess-whose-song-om-anand-1208` Firebase project and run:

```bash
firebase deploy --only hosting
```

The SHA-256 signing certificate in `hosting/.well-known/assetlinks.json` must match the installed APK. Update it when changing from the current APK signing certificate to a release signing key.

## Environment Variables (Server)

| Variable | Description |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `FIREBASE_PROJECT_ID` | Firebase project ID used to verify Android ID tokens (default `guess-whose-song-om-anand-1208`) |
| `REDIS_URL` | Redis connection URL |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin service-account JSON deployment secret |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to a protected Firebase Admin credential file (for local development or a Render secret file); never bake it into a container image |
| `SPOTIFY_CLIENT_ID` | Spotify app Client ID |
| `SPOTIFY_CLIENT_SECRET` | Spotify app Client Secret |
| `SPOTIFY_REDIRECT_URI` | Spotify OAuth redirect URI |

## Architecture

- **Transport**: Authenticated Ktor REST actions with long-poll room events
- **State**: Redis (ephemeral, TTL-based cleanup)
- **Music and artwork**: iTunes Search API (preview clips and album artwork)
- **Identity**: Firebase Anonymous Auth
- **Recovery**: optional Google account linking through Firebase Auth

### Game flow

1. A client signs in anonymously with Firebase and obtains a Firebase ID token.
2. `POST /rooms` creates a room using the verified Firebase UID as the host identity.
3. Other players join with the six-character code through `POST /rooms/{code}/join`.
4. Clients poll `GET /rooms/{code}/events`; the server long-polls for up to 20 seconds and returns queued events.
5. The host starts the game. Players choose songs in manual mode, or the server obtains Spotify tracks in one of the automatic modes.
6. The server resolves playable entries through iTunes, builds the round pool, and broadcasts preview, voting, reveal, and score events.
7. After the configured rounds, `GAME_RESULTS` is sent. The host can play again or end the room.

Rooms, runtime state, rate-limit counters, OAuth state, and chat history are stored in Redis. Chat remains available for the life of the room and is deleted when the room is ended or its TTL expires.

### Song-pool rules

Manual mode requires every human player to contribute at least one playable song. The server resolves title and artist through iTunes and uses iTunes preview URLs and artwork. Two-player rooms receive genre-matched `Nobody / Decoy` tracks; rooms with three or more players do not add a decoy.

For **Recently Played** and **Top Tracks**, each connected player contributes Spotify tracks. A Spotify track enters the playable pool only when iTunes has a match with the same normalized title, artist, and album. Tracks without a usable iTunes preview are skipped, and duplicate recordings or ambiguous ownership are removed before the round pool is built. Two-player Spotify games also receive genre-matched decoys.

The shared rules cap rooms at 10 players and 30 rounds. A two-player game requires at least three rounds so both player songs and a decoy can be represented. Voting timers are 10, 15, 20, or 30 seconds. `RoundPoolBuilder`, `RecentPlayedPoolBuilder`, and `RecentTrackDeduplicator` enforce these rules on the server.

### Backend responsibilities

- `Application.kt` starts Ktor, Redis, authentication, serialization, status handling, and shutdown hooks.
- `RoomManager` owns room creation, lookup, cleanup, host reassignment, and room TTL behavior.
- `RoomSession` is the authoritative state machine for lobby, submission, playing, results, chat, votes, timers, and room end.
- `RoomRepository`, `RoomRuntimeRepository`, and `RoomChatRepository` persist Redis-backed state.
- `MusicService` limits external requests, caches catalog searches, resolves Spotify metadata through iTunes, and selects genre-matched tracks.
- `ItunesClient` is the source for playable previews and artwork.
- `SpotifyOAuth` and `SpotifyClient` handle PKCE, token exchange, top tracks, recent tracks, and playlist reads.
- `FirebaseTokenVerifier` validates Firebase ID tokens before room, music, Spotify, and crash-log operations.
- `RateLimiter` protects room creation, actions, polling, search, Spotify calls, and chat.

## API surface

All routes below require `Authorization: Bearer <Firebase ID token>` unless marked public. The server verifies the token and uses its Firebase UID as the player identity.

| Method | Route | Purpose |
| --- | --- | --- |
| `GET` | `/` | Deployment/root response |
| `GET` | `/health` | Render health check and wake-up probe; public |
| `POST` | `/rooms` | Create a room |
| `GET` | `/rooms/{code}` | Read room summary |
| `POST` | `/rooms/{code}/join` | Join or reconnect a player |
| `DELETE` | `/rooms/{code}/join` | Leave a room |
| `GET` | `/rooms/{code}/events` | Long-poll room events |
| `GET` | `/rooms/{code}/chat` | Read room chat history |
| `POST` | `/rooms/{code}/chat` | Send a room chat message |
| `POST` | `/rooms/{code}/actions` | Send a shared `ClientMessage` action |
| `GET` | `/music/search?q=...` | Search iTunes through the server proxy |
| `GET` | `/music/top` | Get catalog suggestions |
| `GET` | `/spotify/auth-url?client=android` | Start Spotify OAuth with server-side PKCE |
| `GET` | `/spotify/callback` | Complete Spotify OAuth |
| `GET` | `/spotify/status` | Check the current user's Spotify connection |
| `DELETE` | `/spotify/connection` | Remove the current user's Spotify token |
| `GET` | `/spotify/top` | Read the current user's top tracks |
| `GET` | `/spotify/recent` | Read the current user's recently played tracks |
| `GET` | `/spotify/playlists/{id}/tracks` | Read tracks from a Spotify playlist |
| `POST` | `/api/logs/crash` | Submit a bounded authenticated client crash report |

The Android app calls the room and music routes. Spotify access tokens are kept server-side in Redis; they are never placed in an app bundle or room URL.

## Security and deployment notes

- All room, music, Spotify, and crash-log operations require a Firebase ID token. The server uses the verified Firebase UID; legacy client-generated player IDs are not accepted.
- Room sessions use `POST /rooms/{joinCode}/join`, `POST /rooms/{joinCode}/actions`, and authenticated long polling at `GET /rooms/{joinCode}/events`. Clients leave with `DELETE /rooms/{joinCode}/join`.
- Spotify OAuth state and PKCE verifier are generated and stored server-side in Redis for ten minutes and are single-use. Android refreshes authenticated status after the deep-link callback.
- Room state uses the `gws:v2` Redis namespace for the Firebase hard cutover. The deployment is intentionally single-instance until distributed room ownership/pub-sub is implemented.
- See [SECURITY_ROTATION.md](SECURITY_ROTATION.md) for the required credential revocation and Git-history cleanup procedure.

## Render deployment

`render.yaml` defines a single Ktor web service and a private Redis service. The service uses `Dockerfile`, exposes port 8080, and checks `/health`. Render's free service can sleep when idle; the app's health probe can wake it before room creation, but the first request may still take time while the instance starts.

Required Render settings and secrets are:

| Variable | Use |
| --- | --- |
| `FIREBASE_PROJECT_ID` | `guess-whose-song-om-anand-1208`; must match the Android Firebase app and service-account project |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin credentials; deployment secret only |
| `REDIS_URL` | Render Redis connection string |
| `SPOTIFY_CLIENT_ID` / `SPOTIFY_CLIENT_SECRET` | Spotify OAuth |
| `SPOTIFY_REDIRECT_URI` | Server callback URL |

For `FIREBASE_SERVICE_ACCOUNT_JSON`, use a key created in the new Firebase project. As an alternative to a JSON-valued environment secret, Render can mount the same credential as a protected secret file; set `FIREBASE_SERVICE_ACCOUNT_PATH` to that file's runtime path. Do not keep both credential forms in source control.

The deployment intentionally uses one server instance because room ownership is local to the instance. Distributed room ownership and Redis pub/sub would be needed before scaling horizontally.

## CI and artifacts

GitHub Actions is defined in `.github/workflows/security.yml`. It compiles shared code, runs server tests, builds the server fat JAR, assembles the Android debug APK, and uploads `app/build/outputs/apk/debug/app-debug.apk` as a workflow artifact.

CodeQL compiles the server with `SERVER_ONLY=true`, which avoids configuring the Android module during backend analysis.

To run the same core checks locally:

```bash
./gradlew :shared:compileKotlinJvm \
  :server:test \
  :server:shadowJar \
  :app:assembleDebug \
  --no-daemon --stacktrace
```

## APK distribution and signing

The current APK is stored at [`artifacts/GuessWhoseSong.apk`](artifacts/GuessWhoseSong.apk) and can also be uploaded as a GitHub Release asset. Keep keystores, passwords, `google-services.json`, Firebase Admin JSON, Spotify secrets, and Redis credentials out of the repository.

For a production release, use Android Studio's **Build → Generate Signed Bundle / APK** with the release keystore. Back up the keystore because all future updates must use the same signing key. Register the release SHA-1 and SHA-256 fingerprints in Firebase for Google authentication. For Play Store builds, also register the Play App Signing certificate fingerprints.

## Cautions and provider limits

- The iTunes Search API supplies the preview URLs and artwork. Treat its use as non-commercial in this project; obtain permission or replace the provider before a commercial launch.
- The current Spotify developer setup is intended for a maximum of five allowlisted users. Spotify-backed features also require each tester to have Spotify Premium. Confirm Spotify's current developer and product terms before expanding access.
- Spotify tokens are user-specific and expire. The server stores them in Redis and removes them when authorization expires or the user disconnects.
- Keep the Android App Links association file in sync with the signing certificate used by the APK.
- Rooms and chat are ephemeral. Ending a room deletes its Redis state and chat; inactivity cleanup also removes them after their TTL.

## License

This project is licensed under the GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later). See [LICENSE](LICENSE) for the complete terms.
