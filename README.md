# Teamily AI — Android UI Design Package (v36)

This repository is the **UI design package for the Teamily AI Android app**, version **v36**.
It contains the Material 3 (M3) design implementation as Jetpack Compose Kotlin sources plus a
self-contained interactive HTML preview of the same screens.

Everything here is a design/implementation reference — it is not a buildable Android project
(no Gradle module, no manifest). The Kotlin files are the screen-level Compose sources; the HTML
file is a pixel-accurate, clickable preview of the same UI.

---

## What's in here

```
documents/teamily-m3-design_v36/     ← Kotlin (Jetpack Compose) sources, 20 files
webpages/teamily-m3-design_v36/      ← Interactive HTML preview + assets, 46 files
    index.html                       ← the whole preview in one file
    fonts/                           ← Lato 400 / 700 / 900 (woff2, local)
    icons/                           ← cube_01.svg … cube_42.svg
```

### `documents/teamily-m3-design_v36/` — Kotlin sources

| File | What it is |
|---|---|
| `DuaGenAINavHost.kt` | Root nav host wiring all screens together |
| `HomeScreen.kt` | Dua home screen |
| `ChatScreen.kt` | Orhan chat screen + composer |
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
| `SettingsStore.kt` | Settings persistence |
| `DuaGenAINavHostTest.kt` | Compose UI test suite for the nav host and screens |

### `webpages/teamily-m3-design_v36/` — HTML preview

`index.html` is a single self-contained page that renders every screen as a phone mock, in
**light and dark** mode side by side. It is interactive: buttons, dialogs, snackbars, swipe
gestures, long-press menus and tooltips all work in the browser.

---

## How to view it

**The HTML preview (easiest):**

1. Open `webpages/teamily-m3-design_v36/index.html` in any modern browser — no server, no build
   step, no network needed (fonts and icons are local).
2. Or serve it locally:
   ```bash
   cd webpages/teamily-m3-design_v36
   python3 -m http.server 8000
   # then open http://localhost:8000/
   ```
3. Toggle light/dark with the theme switch in the preview header.

**The Kotlin sources:**

Read them directly — they are plain `.kt` files. They reference Material 3 theme tokens
(`MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`) rather than hardcoded colors, so
they are meant to be dropped into a Compose app that provides an M3 theme.

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

**v36** — the latest iteration of the design package.
