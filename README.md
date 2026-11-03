# SOLAR — Native Android Music Streaming & Local Audio Client
**Made by Prithwish**

SOLAR is a production-quality, 100% native Android music streaming and local library player built with **Kotlin**, **Jetpack Compose (Material 3)**, **AndroidX Media3 (ExoPlayer + MediaSessionService)**, **Room**, **DataStore**, and **Firebase Cloud Firestore + Google Credential Manager**.

---

## 1. Visual Identity & Design System
- **Primary Background**: Deep AMOLED Black (`#080808`)
- **Secondary Background**: (`#121212`)
- **Card Surface**: (`#1B1B1B`)
- **Primary Accent**: Dark Orange (`#D35400`)
- **Secondary Accent**: Burnt Orange (`#FF6B1A`)
- **Highlight**: Warm Amber (`#FF9F43`)
- **Typography**: Bundled Google Fonts (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`)
- **Logo**: Custom geometric solar eclipse & soundwave emblem with the developer credit **"Made by Prithwish"** beneath the brand header.

---

## 2. Architecture & Modular Provider System
SOLAR follows **MVVM + Clean Architecture** with isolated provider contracts (`domain/provider/MusicProviders.kt`):
- `MusicProvider`
- `SearchProvider`
- `StreamingProvider`
- `PlaylistProvider`
- `AuthenticationProvider`
- `LyricsProvider`
- `RecommendationProvider`

### Implemented Providers (`data/provider/SolarProviderImpls.kt`)
1. **YouTube Music / InnerTube Discovery & Metadata (`InnerTubeMusicProvider`)**:
   - Primary discovery catalog + live network search via public catalog endpoints, isolated behind `MusicProvider` and `SearchProvider`.
2. **JioSaavn Secondary Provider (`JioSaavnMusicProvider`)**:
   - Secondary regional and independent music discovery provider with graceful fallback.
3. **Local Device MediaStore (`LocalMediaStoreProvider`)**:
   - Full Android `MediaStore.Audio.Media` scanner supporting MP3, FLAC, WAV, AAC, M4A, OGG, and OPUS with folder browsing, hidden folder exclusion, and sorting.
4. **Synchronized LRC Lyrics (`LrcLibLyricsProvider`)**:
   - Fetches time-synced LRC (`[mm:ss.xx]`) and plain lyrics from LRCLIB (`https://lrclib.net/api/get`) with tap-to-seek and auto-highlighting.

---

## 3. Search Accuracy & Official Master Scoring Engine
`SearchScoringEngine` evaluates every search result across 7 weighted signals:
1. Exact or prefix title match
2. Artist match
3. Album match
4. Official artist verification
5. Duration similarity (penalizing <60s shorts/clips)
6. Recording classification (`ORIGINAL`, `LIVE`, `ACOUSTIC`, `REMIX`, `COVER`, `INSTRUMENTAL`, `UNOFFICIAL`)
7. Provider confidence

By default, fan edits, shorts, karaoke, nightcore, slowed+reverb, and unofficial uploads are demoted/filtered out unless explicitly requested in the search query.

---

## 4. Background Playback (`SolarMediaSessionService`)
- Powered by **AndroidX Media3 (`ExoPlayer` + `MediaSessionService`)**.
- Continues playback when minimized or with the screen locked.
- Publishes lock-screen and notification transport controls.
- Automatically manages audio focus, audio ducking, and pauses when headphones disconnect (`setHandleAudioBecomingNoisy(true)`).
- Integrated **5-Band Hardware Equalizer**, **Bass Boost**, and **3D Virtualizer** bound to `ExoPlayer.audioSessionId`.

---

## 5. Cloud Synchronization & Security Rules
- **Authentication**: Google Sign-In via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` & `GetGoogleIdOption`), plus full offline/local **Guest Mode**.
- **Cloud Database**: Firebase Cloud Firestore (`R.string.firestore_database_id`) with zero-trust security rules (`firestore.rules`) enforcing strict per-user isolation across `/users/{userId}`, `/liked_songs/{songId}`, `/playlists/{playlistId}`, and `/listening_history/{historyId}`.

---

## 6. Build & Verification Instructions
- **Compile Debug APK**:
  ```bash
  gradle assembleDebug
  ```
- **Run Firestore Security Rules Tests**:
  ```bash
  FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
  ```
