# Music

A free, open-source YouTube Music client for Android, built from the ground up on
Jetpack Compose with the **Material 3 Expressive** design language and **Liquid Glass**
floating layers. No ads, no tracking, no login required — streams are resolved through
YouTube's public innertube endpoints and played with Media3/ExoPlayer.

**Package:** `com.abhiram.music` · **minSdk 26** (Android 8.0) · **target/compileSdk 37**

## Features

* **25 screens** on a single Compose `NavHost`: home, library (songs / artists / albums /
  playlists), search, artist / album / playlist detail pages, a local-playlist editor,
  built-in playlists (Favorites, Offline, Downloaded, History, Top played, Cache),
  the full-screen player with lyrics, queue, sleep timer, playback stats and error
  views, plus seven settings pages (general, player, gesture, cache, database, other, about).
* **Material Expressive + Liquid Glass** — bouncy spring motion, morphing shapes, a
  floating glass toolbar and pill mini-player (real backdrop blur on Android 13+,
  graceful tint-and-border fallback below).
* **Innertube backend** — search with suggestions, home feed, explore/charts, albums,
  artists, playlists, related tracks, synced lyrics and transcripts, library
  add/remove and likes. All calls return `kotlin.Result` and are suspend-based.
* **Media3 playback** — background `MediaSessionService` with media notification,
  lock-screen controls, audio focus, becoming-noisy handling, skip-silence and
  loudness-normalization toggles, repeat modes, and automatic radio-style queue
  continuation.
* **Disk caching** — `SimpleCache` stores streams in 512 KiB chunks with an LRU
  evictor; the cache size is configurable in settings (default 2 GiB, unlimited option).
* **Local library (Room)** — liked songs, local & cached remote playlists, play
  history, play counts, search history, per-song stream format metadata and a
  persistent queue.
* **Extras** — sleep timer with default duration, squiggly seekbar, dynamic colour
  (MaterialKolor), pure-black mode, reduce-motion support, and an **in-app updater**
  that checks GitHub releases.
* Android 13+ notification permission flow, predictive back
  (`enableOnBackInvokedCallback`), RTL-aware layout, landscape/tablet-friendly.

## Modules

| Module   | Contents |
|----------|----------|
| `:app`   | The entire application: Compose UI, Room database, Media3 `PlayerService`, DI service locator, DataStore preferences, GitHub updater. |
| `:innertube` | Kotlin/HTTP client for YouTube's innertube API (`com.abhiram.innertube`): `YouTube` entry point, request/response models and page parsers. |

Dependency injection is manual (a `ServiceLocator` + `AppContainer`) — no Hilt/Koin.

## Building

This repository is **source-only**: it ships no binaries and no prebuilt APKs.
You have two ways to get a build:

1. **GitHub Actions (recommended)** — every push and pull request runs the
   [Build Debug workflow](.github/workflows/build-debug.yml) (`./gradlew :app:assembleDebug`),
   and the resulting `app-debug.apk` is attached to the run as an artifact.
2. **Locally** — requires JDK 21; Gradle 9.7.1 comes from the wrapper:

   ```sh
   ./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
   ```

### Update checker repository

The in-app updater queries GitHub releases for the repository configured via the
`GITHUB_REPOSITORY` build-config field in [`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
buildConfigField("String", "GITHUB_REPOSITORY", "\"abhiram/Music\"")
```

Change it to your own `owner/repo` when forking so the updater tracks your releases.

## License & credits

Licensed under the **GNU General Public License v3.0** — see [`LICENSE`](LICENSE) for details.

This project follows the lineage of [Metrolist](https://github.com/MetrolistGroup/Metrolist)
and InnerTune, itself built on YouTube Music's innertube API. The innertube client in
`:innertube` derives from that family of projects; Material 3 Expressive theming builds
on the Compose Material 3 and material-kolor ecosystems.
