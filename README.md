# Guess Whose Song

A multiplayer party quiz app. Players submit songs anonymously. Everyone votes on who submitted each clip. Correct guesses earn points.

## Project Structure

```
MusicAppProject/
├── shared/   # Kotlin Multiplatform — DTOs shared between server and Android
├── server/   # Ktor backend (game engine, Redis, Deezer/Spotify/Firebase)
└── app/      # Android client (Jetpack Compose)
```

## Setup

### 1. Firebase
- Create a Firebase project at https://console.firebase.google.com
- Enable **Anonymous Authentication**
- Enable **Firebase Cloud Messaging**
- Enable **Crashlytics** and **Analytics**
- Download `google-services.json` and replace `app/google-services.json`
- For the server: download a service account key and set `FIREBASE_SERVICE_ACCOUNT_PATH` env var (or `GOOGLE_APPLICATION_CREDENTIALS`)

### 2. Spotify (Optional — for Surprise Me feature)
- Create an app at https://developer.spotify.com/dashboard
- Set the redirect URI to your server URL + `/spotify/callback`
- Set env vars: `SPOTIFY_CLIENT_ID`, `SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REDIRECT_URI`

### 3. Redis
- Local: `redis-server` (default port 6379)
- Production: set `REDIS_URL` env var (e.g. Render Redis add-on URL)

### 4. Server
```bash
./gradlew :server:shadowJar
java -jar server/build/libs/guess-whose-song-server.jar
```

### 5. Android
- Open in Android Studio
- Update `BASE_URL` in `NetworkModule.kt` to point to your server
- Run on device or emulator

## Environment Variables (Server)

| Variable | Description |
|---|---|
| `PORT` | HTTP port (default 8080) |
| `REDIS_URL` | Redis connection URL |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Path to Firebase service account JSON |
| `SPOTIFY_CLIENT_ID` | Spotify app Client ID |
| `SPOTIFY_CLIENT_SECRET` | Spotify app Client Secret |
| `SPOTIFY_REDIRECT_URI` | Spotify OAuth redirect URI |

## Architecture

- **Transport**: WebSockets (Ktor) for real-time game events
- **State**: Redis (ephemeral, TTL-based cleanup)
- **Audio**: Deezer public API (30-second preview clips, keyless)
- **Identity**: Firebase Anonymous Auth
- **Notifications**: Firebase Cloud Messaging
