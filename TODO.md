# Music Streaming App (com.abhiram.music) — Build TODO

> Live checklist — the lead updates this file as **every** job finishes.
> Rules: source files only (never compile/build on this machine).
> Build/CI rules supersede the old "no commit/push" rule — per `$HOME/build.txt`: push to the
> **private** repo, compile on GitHub Actions, poll every 10 s, fix from failure logs, repeat
> until green, then download the APK artifact to `/data/data/com.termux/files/home/`.
> Temporary rule in force until the first green build: **no sub-agents; all edits/writes via bash**.

**Status legend:** [ ] todo · [~] in progress · [x] done

---

## Wave 0 — Foundation (lead) ✅ ALL DONE
- [x] Project skeleton: settings.gradle.kts, root build, libs.versions.toml, gradle.properties, .gitignore
- [x] app module gradle config (minSdk 26 / compileSdk 37 / compose / room+ksp / media3 / ktor) + proguard
- [x] Gradle 9.7.1 wrapper copied (gradlew, gradle-wrapper.jar/.properties)
- [x] innertube module copied into project, re-packaged → `com.abhiram.innertube` (92 kt files)
- [x] Material3 1.5.0-alpha28 API verified from AAR (MaterialShapes, MotionScheme, LoadingIndicator, SplitButton; no `dim` roles → custom DimTokens)
- [x] Core contracts: `MediaMetadata`, `BuiltInPlaylist`, `PlayerController.kt`, `CONTRACTS.md`

## Wave 1 — Parallel foundation jobs ✅ ALL DONE
- [x] A1 innertube module integration (lead did the work)
- [x] A2 Core models + repositories (13 files) — done 14:44
- [x] A3 Room database (25 files, 86 DAO methods) — done 14:52
- [x] A4a Theme `ui/theme` (ExpressiveTheme/Typography/Shapes/DimTokens/Motion, 919 L) — done
- [x] A4b Glass `ui/glass` (GlassBackdrop/LiquidGlassSurface/GlassModifier, 682 L) — done; superseded the lead's draft `Glass.kt` (redeclaration risk removed)
- [x] A5 PlayerService + ResolvingDataSource + RingBuffer + YoutubeRadio (6 files, 1502 L) — done 15:28
- [x] A6 PlayerControllerImpl (586 L, 24 frozen methods) + service wiring — done 15:28
- [x] A7a Frozen 9 components (1038 L) · A7b Scaffolds 7 (ChipScaffold/HomeScaffold/CoverScaffold/TabScaffold/TabGroup/BottomNavigation/LoadingIndicator, ~1090 L) — done
- [x] A8 Menus (7 files, 1373 L) — done 15:05
- [x] A9 Music item components (14 files) — done 15:10
- [x] A10 Player components (11 files, 3641 L) — done 15:20
- [x] A11 Search + Settings (18 files) — done 15:12 (+ lead fixes: cold-start play, repo URLs, clear-history wired)
- [x] A12 CI workflow + manifest + res + README + lint.xml (16 files) — done 14:38
- [x] A13 Lead glue: FormatUtils/resize, GitHubUpdateChecker, Routes, App.kt, MainActivity, UpdateDialog

## Wave 2 — Screens (all reports received) ✅ ALL DONE
- [x] B1 CMP-D screen components (38–64): 17 contract composables across 11 files — done (machine-diffed §6.1)
- [x] B2 SCR-L home + library (8 files, ~2157 L): HomeScreen/ViewModel + 4 library tabs/ViewModels/LibraryUi — done (signatures match AppNavigation)
- [x] B3 SCR-D detail assembly (7 files, ~1496 L): DetailUi, DetailViewModels (5 VMs), 5 detail screens — done (callback-only, 0 NavController imports)

## Wave 3 — Integration (lead, bash-only) ✅ ALL DONE
- [x] Navigation graph: `AppNavigation.kt` rewritten — `AppNavigation` (Scaffold host: floating **MiniPlayer** + glass **BottomNavigation** in `bottomBar`, content inset by measured height, `WindowInsets(0.dp,…)` so screens keep their own insets) + private `AppNavigationGraph` (all 25 routes, spring transitions)
- [x] MainActivity: ExpressiveTheme → GlassBackdrop → AppNavigation → update dialog
- [x] Wrong-package import fix: `com.abhiram.music.navigation.Routes` → `ui.navigation.Routes` (5 detail screens)
- [x] SettingsScreen KDoc `settings/*` nesting bug fixed (would have broken the Kotlin lexer — block comments nest)

## Pre-flight verification (all green — no compiler needed)
- [x] `check_imports.py`: 792 intra-project imports / 1182 symbols / 29 packages — **all resolve**
- [x] `bracket_scan.py` (new, Kotlin-aware incl. nested comments + `${}` templates): **246 files structurally clean**
- [x] Version catalog ↔ build scripts: all accessors resolve (4 gradle files)
- [x] Resources: all `R.*`/`@res` refs resolve (strings/drawables/colors/mipmap incl. manifest `@style/Theme.Music`)
- [x] Manifest: 15 component class refs resolve; `com.google.android.material:material` 1.14.0 present for the XML theme
- [x] No `TODO()`/stubs; 0 duplicate top-level declarations
- [x] Toolchain proven by config-probe run #38585513419 (gradle/SDK/resources/manifest/innertube/deps all OK; only `glass` unresolved at that time — since landed)

## Build pipeline (per $HOME/build.txt)
- [x] gh CLI authenticated (Meoww-83), private repo: https://github.com/Meoww-83/Music
- [x] GITHUB_REPOSITORY buildConfigField → "Meoww-83/Music"
- [ ] **Initial push of full source → GitHub**
- [ ] **GitHub Actions `Build Debug` green** (poll every 10 s, fix logs, repeat)
- [ ] **Download APK artifact → /data/data/com.termux/files/home/**
- [ ] Final TODO close-out

---

### Completion log (live)
- 14:19 — Wave 0 complete (skeleton, wrapper, innertube re-package, contracts)
- 14:26 — Wave 1 launched (A1–A13)
- 15:07 — CI config-probe #1 (run 38585513419): toolchain green, only `ui/glass` missing
- 16:0x — Wave 2 reports in (CMP-D, SCR-L, SCR-D) + theme/glass/components/scaffolds landed
- 16:4x — Wave 3 complete: navigation restructure, import fixes, `bracket_scan.py` created; caught + fixed real `SettingsScreen.kt` KDoc lexer bug
- Oct 15 — All pre-flight gates green; ready for initial push + CI loop
