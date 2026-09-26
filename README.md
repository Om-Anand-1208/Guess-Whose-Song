# Guess Whose Song

A multiplayer party quiz app. Players submit songs anonymously. Everyone votes on who submitted each clip. Correct guesses earn points.

## Project Structure

```
MusicAppProject/
├── shared/   # Kotlin Multiplatform — DTOs shared between server and Android
├── server/   # Ktor backend (game engine, Redis, Deezer/Spotify/Firebase)
├── app/      # Android client (Jetpack Compose)
└── web/      # Kotlin/Wasm Compose browser client (Firebase Hosting)
```

## Setup

### 1. Firebase
- Create a Firebase project at https://console.firebase.google.com
- Enable **Anonymous Authentication**
- Enable **Google** sign-in. Guests may link Google later; linking preserves the anonymous UID and a collision is rejected (accounts are never merged automatically).
- Enable **Firebase Cloud Messaging**
- Enable **Crashlytics** and **Analytics**
- Download `google-services.json` and replace `app/google-services.json`
- For the server: provide the service-account JSON through the deployment secret `FIREBASE_SERVICE_ACCOUNT_JSON`. A local file path is supported only for local development; never copy credentials into Docker or commit them.

### 2. Spotify (Optional — for Surprise Me feature)
- Create an app at https://developer.spotify.com/dashboard
- Set the redirect URI to your server URL + `/spotify/callback`
- Set env vars: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`

### 3. Redis
- Local: `redis-server` (default port 6379)
- Production: set `REDIS_URL` env var (including `rediss://` for TLS where required, e.g. Render Redis add-on URL)

### 4. Server
```bash
./gradlew :server:shadowJar
java -jar server/build/libs/guess-whose-song-server.jar
```

Run only one Ktor server on port 8080. If it is already running in another terminal or tmux pane, reuse it; stop that process before starting another instance, or the second start will fail with `Address already in use`. The ngrok tunnel can remain running while the server is restarted.

### 5. Android
- Open in Android Studio
- Update `BASE_URL` in `NetworkModule.kt` to point to your server
- Run on device or emulator

### 6. Web

The browser client uses Kotlin/Wasm and Compose Multiplatform. It signs users in anonymously through Firebase, then optionally links Google without changing the Firebase UID. Copy `web/firebase-config.example.js` to `web/src/wasmJsMain/resources/firebase-config.local.js`, fill in the public Firebase web configuration and API/WebSocket origins, and copy it over `web/src/wasmJsMain/resources/firebase-config.js` for a local build.

```bash
./gradlew :web:wasmJsBrowserDevelopmentRun
```

For Firebase Hosting:

1. Enable Anonymous and Google providers in Firebase Authentication.
2. Add the Firebase Hosting domain to Authentication authorized domains.
3. Copy `.firebaserc.example` to `.firebaserc` and set the Firebase project ID.
4. Set the production values in `web/src/wasmJsMain/resources/firebase-config.js` or inject the public config during CI.
5. Build and deploy:

```bash
./gradlew :web:wasmJsBrowserDistribution
firebase deploy --only hosting
```

The hosting output is `web/build/dist/wasmJs/productionExecutable`. Room connections use a six-character code and display name; credentials and display names are never placed in the URL. Since browser WebSockets cannot set custom Authorization headers, the web client first obtains a 30-second single-use ticket over authenticated HTTP and presents it as a WebSocket subprotocol.

CI can inject the complete public `firebase-config.js` contents through the `WEB_FIREBASE_CONFIG` repository secret. Firebase web configuration is client-visible; Firebase Admin service-account credentials must never be placed in this file or any browser bundle.

## Environment Variables (Server)

| Variable | Description |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `FIREBASE_PROJECT_ID` | Firebase project ID used to verify browser ID tokens (default `guess-whose-song`) |
| `REDIS_URL` | Redis connection URL |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin service-account JSON deployment secret |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Local-only fallback path; do not use in container images |
| `SPOTIFY_CLIENT_ID` | Spotify app Client ID |
| `SPOTIFY_CLIENT_SECRET` | Spotify app Client Secret |
| `SPOTIFY_REDIRECT_URI` | Spotify OAuth redirect URI |
| `SPOTIFY_WEB_REDIRECT_URI` | Fixed web URL returned after Spotify OAuth; never accept arbitrary redirect targets |
| `WEB_ORIGINS` | Comma-separated allowed browser origins; unset means no browser origins are allowed |

## Architecture

- **Transport**: WebSockets (Ktor) for real-time game events
- **State**: Redis (ephemeral, TTL-based cleanup)
- **Audio**: Deezer public API (30-second preview clips, keyless)
- **Identity**: Firebase Anonymous Auth
- **Recovery**: optional Google account linking through Firebase Auth
- **Notifications**: Firebase Cloud Messaging
- **Web**: Kotlin/Wasm + Compose Multiplatform, hosted by Firebase Hosting

## Security and deployment notes

- All room, music, Spotify, crash-log, and WebSocket operations require a Firebase ID token. The server uses the verified Firebase UID; legacy client-generated player IDs are not accepted.
- WebSockets authenticate with `Authorization: Bearer <Firebase ID token>` and then require a first `JOIN_ROOM` frame containing only the display name.
- Browser WebSockets use `POST /rooms/{joinCode}/ws-ticket` with a Firebase bearer token, followed by the negotiated `gws-ticket` marker plus the single-use `gws-ticket.<ticket>` subprotocol and the same `JOIN_ROOM(displayName)` first frame. Native clients continue using the Authorization header. Tickets are bound to the verified UID and room code and are atomically consumed in Redis.
- Spotify OAuth state and PKCE verifier are generated and stored server-side in Redis for ten minutes and are single-use. Android refreshes authenticated status after the deep-link callback.
- Room state uses the `gws:v2` Redis namespace for the Firebase hard cutover. The deployment is intentionally single-instance until distributed room ownership/pub-sub is implemented.
- See [SECURITY_ROTATION.md](SECURITY_ROTATION.md) for the required credential revocation and Git-history cleanup procedure.
