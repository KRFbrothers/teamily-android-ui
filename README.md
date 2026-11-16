# Dua Gen AI — Android UI Design Package (v01)

CRITICAL: This repository is 100% Kotlin Jetpack Compose (`app/src/main/java/ai/duagenai/design/`). Never create `.html` prototype files. Use `@Preview(widthDp = 390, heightDp = 844)` and `./gradlew :app:assembleDebug` for all previews.

This repository is the **UI design package for the Dua Gen AI Android app**, version **v01**.
The Material 3 (M3) screens are Jetpack Compose Kotlin in the `app/` module
(`ai.duagenai.design`), with a Gradle wrapper, an `AndroidManifest.xml`, and the theme /
cube / navigation types the screens reference. See [Building](#building) below.

---

## Overview

Dua Gen AI — ek complete package: Dua AI, Dua Agent aur Dua Workspace.

## Project parts

- **Dua AI**
- **Dua Agent**
- **Dua Workspace**

---

## What's in here

```
app/src/main/java/ai/duagenai/design/   ← screens, nav, theme (source of truth)
app/src/main/res/font/                  ← Lato Regular / Bold, wired by DuaGenAITheme
app/src/main/res/drawable/              ← user avatar and 3D bot badge
documents/M3-design_v01/                ← ARCHIVED snapshot (do not edit)
```

> **`documents/M3-design_v01/` is archived.** It is the original design snapshot,
> frozen and no longer maintained — see
> [`documents/M3-design_v01/ARCHIVED.md`](documents/M3-design_v01/ARCHIVED.md).
> Take files from `app/src/main/` instead; v01's vulnerable JSON-building code has
> been patched in place so it cannot be copied back in.

### `documents/M3-design_v01/` — Kotlin sources (archived)

| File | What it is |
|---|---|
| `DuaGenAINavHost.kt` | Root nav host wiring all screens together |
| `HomeScreen.kt` | Dua home screen |
| `ChatScreen.kt` | Agent chat screen + composer |
| `ComposerSheets.kt` | Composer sheets: Attach (+) Quick Actions, Skills (5 Dua Agent skills), MCP Tools (master toggle + built-in tools) |
| `MemoryScreen.kt` | Memory list, filter chips, search, detail dialog (Open source / Copy text / Share / Close) |
| `ArtifactsScreen.kt` | Artifacts grid, filter, pull-to-refresh, swipe-to-delete, context menu |
| `CubePickerScreen.kt` | Cube picker with search/filter chips (48dp) + long-press tooltip |
| `ProfileScreen.kt` | Profile screen and its rows |
| `AppearanceScreen.kt` | Appearance settings |
| `StorageScreen.kt` | Storage settings |
| `NotificationsScreen.kt` | Notifications settings |
| `SidebarDrawer.kt` | Sidebar navigation drawer |
| `BaseRemoteRepository.kt` | Shared generic repository base (load/refresh/add/delete/retry) |
| `ArtifactsRepository.kt` | Artifacts repository on top of the base class |
| `MemoryRepository.kt` | Memory repository on top of the base class |
| `JsonHttpClient.kt` | Shared JSON HTTP client |
| `ApiConfig.kt` | Shared base URL / API config |
| `UrlOpener.kt` | Shared URL-opening helper |
| `TouchTargets.kt` | 48dp minimum touch-target helper |
| `TeamilyTokens.kt` | Forest-slate glass palette (light/dark) and card / composer / dock shapes |
| `TeamilyTypography.kt` | Teamily type scale. `DuaGenAITheme` passes Lato from `res/font/` |
| `TeamilyEffects.kt` | Shadows, glass blur, press scale, and motion |
| `SettingsStore.kt` | Settings persistence |
| `DuaGenAINavHostTest.kt` | Compose UI test suite for the nav host and screens |

## How to view it

Preview a screen with `@Preview(widthDp = 390, heightDp = 844)`, or build the app:

```bash
./gradlew :app:assembleDebug
```

The sources use Material 3 theme tokens (`MaterialTheme.colorScheme.*`,
`MaterialTheme.typography.*`). `DuaGenAITheme` supplies the palette and Lato.

---

## Building

The Android module lives in `app/` and is built with the Gradle wrapper:

```bash
./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:testDebugUnitTest  # run the Compose UI test suite (Robolectric)
```

Requirements: JDK 17 and the Android SDK (platform 34, build-tools 34). Point Gradle at the
SDK with `ANDROID_HOME` or a `local.properties` file containing `sdk.dir=/path/to/sdk`.

### Where the sources live

| Path | What it is |
|---|---|
| `app/src/main/java/ai/duagenai/design/screens/` | Screens, repositories, HTTP client, settings |
| `app/src/main/java/ai/duagenai/design/cube/` | Cube catalogue, avatar, picker, saved-avatar store |
| `app/src/main/java/ai/duagenai/design/theme/` | Palette, type scale, shapes, effects, `DuaGenAITheme` |
| `app/src/main/java/ai/duagenai/design/nav/` | `DuaGenAINavHost` + `MainActivity` |
| `app/src/test/java/ai/duagenai/design/nav/` | Compose UI test suite |
| `documents/M3-design_v01/` | Archived original snapshot — do not edit; `app/src/main/` is the source of truth |

---

## Design notes

- **Material 3 only** — all styling goes through M3 theme tokens; no hardcoded colors.
- **Light and dark** — every screen is designed for both modes.
- **Accessibility** — interactive controls carry content descriptions and a minimum 48dp touch
  target (`TouchTargets.kt`).
- **Shared infrastructure** — one HTTP client (`JsonHttpClient`), one base URL (`ApiConfig`), one
  URL opener (`UrlOpener`), and one generic repository base class (`BaseRemoteRepository`) shared
  by both Artifacts and Memory.

---

## Version

**v01** — the latest iteration of the design package.

**Buildable module** — the `app/` module was added on top of v01: the missing Gradle module,
manifest, theme, cube and navigation types, plus fixes for the review's blockers (JSON built
with `JSONObject` instead of string interpolation, percent-encoded ids in DELETE paths,
`CancellationException` rethrown instead of swallowed, failed writes reported instead of
silently dropped, and the chat transcript moved out of `rememberSaveable` into a retained
`ViewModel`).
