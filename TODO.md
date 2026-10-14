# Music Streaming App (com.abhiram.music) — Build TODO

> Live checklist — the lead updates this file as **every** job finishes.
> Rules: source files only (never compile/build on this machine), no git commit/push.

**Status legend:** [ ] todo · [~] in progress · [x] done

---

## Wave 0 — Foundation (lead) ✅ ALL DONE
- [x] Project skeleton: settings.gradle.kts, root build, libs.versions.toml, gradle.properties, .gitignore
- [x] app module gradle config (minSdk 26 / compileSdk 37 / compose / room+ksp / media3 / ktor) + proguard
- [x] Gradle 9.7.1 wrapper copied (gradlew, gradle-wrapper.jar/.properties)
- [x] innertube module copied into project, re-packaged `com.metrolist.innertube` → `com.abhiram.innertube` (95 files, verified no stale refs)
- [x] Material3 1.5.0-alpha28 API verified from AAR (MaterialShapes, MotionScheme, LoadingIndicator, SplitButton present; no `dim` roles → custom DimTokens)
- [x] Core contracts written: `model/MediaMetadata.kt`, `model/BuiltInPlaylist.kt`, `playback/PlayerController.kt`, `CONTRACTS.md` (ownership + frozen signatures)

## Wave 1 — Parallel foundation jobs (10 agents launched)
- [~] A1 innertube module integration (copy + re-namespace + build file) — **lead did the work, agent verification folded into final review**
- [x] A2 Core data models + repositories (di/, repository/, constants/, SleepTimer, PlaybackStats) — **done 14:44** (13 files, all signatures verified against innertube + DAO sources; 5 integration notes recorded)
- [~] A3 Room database (entities, relations, DatabaseDao, MusicDatabase, converters)
- [~] A4 Theme: expressive tokens + dim tokens + liquid glass layer
- [~] A5 PlayerService: ResolvingDataSource, SimpleCache, RingBuffer, YoutubeRadio, notification
- [~] A6 PlayerControllerImpl + service wiring (inside A5), DI/preferences (inside A2)
- [~] A7 Shared components: scaffolds, dialogs, indicators, bottom navigation (★ frozen signatures)
- [~] A8 Menus: Base/MediaItem/Queued/NonQueued/InPlaylist/InHistory + MenuEntry — *first agent returned empty, retry launched 14:38*
- [~] A9 Music item components (23–37): ItemContainer, ListItemContainer, Song/Video/Album/Artist/Playlist + Local* + placeholders
- [~] A10 Player components (53–62): MiniPlayer, Player, Controls, Lyrics, Queue, SleepTimer, StatsForNerds, PlaybackError, Thumbnail, PlayerScaffold
- [~] A11 Search + Settings screens/components (65–80)
- [x] A12 GitHub Actions debug workflow, AndroidManifest, res base, README, lint.xml — **done 14:38** (16 files; reported Material XML theme dependency requirement → lead fix queued)
- [x] A13 Lead glue: FormatUtils(+resize), GitHubUpdateChecker, Routes, App.kt, UpdateDialog

## Wave 2 — Screen component + screen agents (CMP-D, SCR-L, SCR-D launched 14:47)
- [~] B1 Album/Artist/Playlist/Built-in/Local playlist + Home components (38–64) — CMP-D agent
- [~] B2 Home screen + library tabs (Songs/Artists/Albums/Playlists) + viewmodels — SCR-L agent
- [~] B3 Detail screen assembly (Artist/Album/Playlist/Local/Built-in) + viewmodels — SCR-D agent

## Build pipeline (per $HOME/build.txt)
- [x] gh CLI authenticated (Meoww-83), private repo created: https://github.com/Meoww-83/Music
- [x] GITHUB_REPOSITORY buildConfigField → "Meoww-83/Music"
- [ ] Initial push of source to GitHub
- [ ] GitHub Actions `Build Debug` run green (poll every 10s, fix logs, repeat)
- [ ] Download APK artifact → /data/data/com.termux/files/home/

## Wave 3 — Integration (lead)
- [ ] Navigation graph (routes per CONTRACTS §8) + MainActivity + App wiring
- [ ] Update dialog wiring on app open (GitHub release check)
- [ ] Cross-file consistency review pass (contract deviations, imports, signatures)
- [ ] Final verification: no builds run, workflow present, TODO closed out

---

### Completion log (live)
- 14:19 — Wave 0 complete (skeleton, wrapper, innertube re-package, contracts)
- 14:26 — Wave 1 launched: 10 parallel agents (A1–A12 above)
