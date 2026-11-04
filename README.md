# Teamily AI — Android App UI Design

A **UI design reference** for the Teamily AI Android app: 14 screens, Material 3,
day + night themes, in a single self-contained HTML showcase.

> **This repository contains design only.** There is no application code here —
> no Kotlin, no Gradle, no `AndroidManifest.xml`, no backend. It is a visual and
> token reference for whoever implements the app.

---

## What's in here

```
design/
  index.html          the whole showcase — 14 screens, day + night, one file
  images/             10 brand assets (logo, wordmark, agent + user avatars)
  fonts/              Lato 400 / 700 / 900 (woff2, self-hosted)
DESIGN_SPEC.md        the written specification the showcase implements
```

`design/index.html` is fully self-contained: it references only the local
`images/` and `fonts/` folders. No CDN, no network, no build step.

---

## How to open it

Open `design/index.html` in any modern browser:

```bash
# macOS
open design/index.html

# Linux
xdg-open design/index.html

# Windows
start design\index.html
```

Or just double-click the file.

**Switching screens.** On a wide window (≥1081 px) a numbered rail on the left
lists all 14 screens — click any of them. On a narrow window the page becomes a
single phone and you navigate with the app's own bottom navigation and in-app
buttons, exactly as the real app would.

**Switching theme.** Three ways, all live:

- the sun/moon button in any app bar,
- **Settings → Appearance**, which offers System / Day / Night plus a preview,
- or let it follow your OS setting (System mode).

---

## The 14 screens

| # | Screen | Purpose |
|---|---|---|
| 01 | Home / Chats | The default landing screen. A single inbox where agent threads, 1-to-1 chats and group chats coexist. |
| 02 | Chat | A single agent (or person/group) thread with inline artifacts. |
| 03 | Agents | Manage the user's agent roster. |
| 04 | Discover | Find and install new agents. |
| 05 | Profile | Identity, quick actions and the entry point to all settings. |
| 06 | Settings | Every setting, grouped. |
| 07 | Appearance / Theme | The only place all three theme modes are selectable, plus a live preview. |
| 08 | Edit profile | Edit display name, photo, handle and bio. |
| 09 | Notifications | Push/email preferences and quiet hours. |
| 10 | Privacy & security | App lock, 2FA, privacy toggles, blocked users, data controls. |
| 11 | Chat preferences | Composer and thread behaviour. |
| 12 | Storage | Show usage and free space. |
| 13 | Language | App language and agent reply language. |
| 14 | About | Version, legal, support. |

**Bottom navigation** has **4 destinations** — Chats, Agents, Discover, Profile.
Screens 06–14 are sub-screens reached from Profile or Settings and carry no
bottom bar.

---

## Brand tokens

These are invariant — they do **not** change between day and night.

| Token | Value | Use |
|---|---|---|
| Primary | `#087c45` | Brand green, primary actions |
| Primary gradient | `#0b8249` → `#087c45` | Hero surfaces, primary buttons |
| Logo mint | `#42d982` | Logo mark, presence dots, agent spark — **fill only** |
| Brand soft | `#cbe7d2` | Tinted chips, selected nav pill (day) |
| Dark canvas | `#141816` | Night background |
| Light canvas | `#eef2ef` | Day background |
| Ink | `#182b23` | Primary text |
| Agent bubble | `#303433` | AI message bubble (night) |
| User bubble | `#505752` | User message bubble (night) |

**Typography:** Lato, weights **400 / 700 / 900**, self-hosted as woff2.

Full day/night token tables, spacing, radii, elevation, the icon sheet and the
Material 3 → Compose mapping are in **`DESIGN_SPEC.md`**.

---

## Accessibility

- **Touch targets are ≥ 48 px.** Verified across every screen, both themes, at
  360 / 390 / 1440 px.
- **Mint `#42d982` is never used as a text colour.** On the light canvas it
  measures **1.62:1**, far below the 4.5:1 AA threshold. It is a *fill* colour
  only — logo, presence dots, badges, icon strokes. The showcase was checked
  programmatically for any mint-coloured text node and found none.
- **Exactly one `aria-current="page"`** per screen that owns a bottom-nav
  destination; sub-screens carry none.
- Text/background pairs meet WCAG AA; the measured ratios are tabulated in
  `DESIGN_SPEC.md`.

---

## Verification

The committed `design/index.html` was rendered in headless Chromium and measured
across **14 screens × 2 themes × 3 widths = 84 combinations**:

| Check | Result |
|---|---|
| Combinations measured | **84 / 84** |
| Horizontal overflow (document + screen) | **0** |
| Elements spilling outside the phone frame | **0** |
| Touch targets below 48 px | **0** |
| Console errors / page errors / failed requests | **0 / 0 / 0** |
| `aria-current="page"` per nav screen | **exactly 1** |
| Mint used as a text colour | **0** |
| Theme actually applied per combination | **84 / 84** |

A real-interaction pass at 390 px also confirmed the phone UI navigates by
clicking: Home → Agents → Discover → Profile → Settings → Appearance, and that
the Appearance cards and the app-bar toggle both flip the theme.

**Not verified:** rendering on a physical Android device or in a Compose
runtime. The checks above are of the HTML design reference, not of the app.

---

## Source

The design was taken from the published Teamily AI design pages and the written
specification:

- https://static.teamily.ai/sites/a92f0e27-0c1b-40b9-a4fc-6c4aba860bbf/webpages/teamily-android-app_v2/index.html
- https://static.teamily.ai/sites/a92f0e27-0c1b-40b9-a4fc-6c4aba860bbf/webpages/teamily-android-app/index.html
- https://static.teamily.ai/sites/b99346af-156c-4cf3-834e-a0fd7c9da731/documents/teamily-design/TEAMILY_ANDROID_UI_DESIGN.md

`design/index.html` is a byte-identical copy of the `_v2` page, and every image
and font is byte-identical to the asset it was fetched from. Nothing was added,
redrawn or invented.

The earlier `teamily-android-app` page is a **subset** of `_v2` — it contains
only 3 of the 14 screens (Home, Chat, Agents). `_v2` is the complete set, so it
is the one shipped here.
