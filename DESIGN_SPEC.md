# Teamily AI — Android App UI Design Specification

**Version:** 2.0 · **Platform:** Android (Jetpack Compose, Material 3) · **Screens:** 14 · **Base width:** 390 px
**Source of truth:** `teamily-android-app_v2` mockup — brand tokens read from Teamily's production CSS, not approximated.
**Audience:** Android developer implementing this design.

---

## 0. How to read this document

Every value here was extracted from the delivered mockup (`teamily-android-app_v2/index.html`) — colour values, type sizes, radii, spacing, component dimensions. Nothing is invented. Where the mockup did not state a value, this document says so explicitly in a **⚠ Not specified** note rather than guessing.

Values are given in **px at the 390 px design base**. For Compose, convert 1 px → 1 dp at 1× density (the mockup is a 1:1 dp representation at 390 dp wide). So `48px` in the mockup = `48.dp` in Compose.

---

## 1. Design system

### 1.1 Brand tokens (invariant — do not theme-vary)

These nine values are the brand. They do **not** change between day and night mode.

| Token | Value | Role |
|---|---|---|
| Primary | `#087c45` | Teamily green. The brand anchor. |
| Primary gradient | `#0b8249` → `#087c45` (180°) | Primary buttons, FAB, user bubble in day mode |
| Logo mint | `#42d982` | Logo accent, agent presence dot, active accents |
| Brand soft | `#cbe7d2` | Soft green fills, primary surface in night mode |
| Dark canvas | `#141816` | Night-mode page background |
| Light canvas | `#eef2ef` | Day-mode page background |
| Ink | `#182b23` | Primary text in day mode |
| Agent bubble (night) | `#303433` | Incoming agent message surface at night |
| User bubble (night) | `#505752` | Outgoing message surface at night |

**Note on the user bubble:** in **day** mode the user bubble is the brand green `#087c45` with white text. In **night** mode it is neutral `#505752` with near-white text. This is deliberate — full-saturation green at night is visually loud, so night mode desaturates to a neutral grey while keeping the brand green for accents and the agent.

### 1.2 Typography

**Typeface:** Lato, self-hosted. Three weights only.

| Weight | File | Use |
|---|---|---|
| 400 Regular | `lato-regular` | Body copy, message bubbles, secondary labels |
| 700 Bold | `lato-bold` | Screen titles, row titles, buttons, section headers |
| 900 Black | `lato-black` | Display name only — one per screen |

**⚠ Important:** ship Lato as **subset** files (Latin + the 6 language scripts listed in §3.13). The full family is ~4.8 MB; a subset is ~180 KB. Do not bundle the full TTF set.

**Type scale** (extracted sizes, mapped to roles):

| Role | Weight | Size | Line height | Letter spacing | Usage |
|---|---|---|---|---|---|
| Display / profile name | 900 | 21 px | 1.2 (25 px) | −0.02 em | Profile header, one per screen |
| Screen title | 700 | 17 px | 1.35 (23 px) | −0.01 em | App bar / detail bar title |
| Section header | 700 | 13 px | 1.4 | 0.06 em | Uppercase group labels ("Account", "Look & feel") |
| Row title | 700 | 15 px | 1.45 (22 px) | −0.01 em | Settings row primary text |
| Body / bubble | 400 | 14.5 px | 1.55 (22.5 px) | 0 | Message bubbles, descriptions |
| Row subtitle | 400 | 12.5 px | 1.45 (18 px) | 0 | Settings row secondary text |
| Chip label | 700 | 12 px | 1 | 0.02 em | Filter chips |
| Caption / metadata | 400 | 11.5 px | 1.4 | 0.01 em | Timestamps, counts |
| Overline / badge | 700 | 10 px | 1 | 0.1 em | AGENT badge, unread count |

The mockup uses sizes 9.5 / 10 / 10.5 / 11 / 11.5 / 12 / 12.5 / 13 / 13.5 / 14 / 14.5 / 15 / 15.5 / 16 / 17 / 18 / 19 / 20 / 21 / 22 px across its internals. The table above is the **canonical set** — implement these nine roles and you cover every screen. Do not add a tenth size.

### 1.3 Spacing

Base unit 4 px. The scale actually used in the mockup:

| Token | Value | Typical use |
|---|---|---|
| `space-1` | 4 px | Icon-to-label gap inside a chip |
| `space-2` | 6 px | Tight stack (bubble metadata) |
| `space-3` | 8 px | Row internal gap |
| `space-4` | 10 px | Card padding (small) |
| `space-5` | 12 px | List item vertical padding |
| `space-6` | 14 px | Bubble padding |
| `space-7` | 16 px | **Screen horizontal gutter** |
| `space-8` | 18 px | Section gap |
| `space-9` | 20 px | Card padding (large) |
| `space-10` | 22 px | Group separation |
| `space-11` | 28 px | Empty-state block gap |

**Screen gutter is 16 px on both sides** — every screen, no exceptions. List rows are full-bleed to the gutter, with their internal padding carrying the visual inset.

### 1.4 Corner radius

| Token | Value | Applied to |
|---|---|---|
| `radius-xs` | 5 px | Small inner elements, bubble tail corner |
| `radius-sm` | 11 px | Chips |
| `radius-md` | 14 px | Message bubbles (main corners) |
| `radius-lg` | 16 px | Cards |
| `radius-xl` | 20 px | Settings groups, sheet inner cards |
| `radius-2xl` | 26 px | Bottom sheet top corners |
| `radius-3xl` | 28 px | Composer field, large cards |
| `radius-4xl` | 32 px | Hero container |
| `radius-pill` | 999 px | Search field, chips in a row, active indicator |
| `radius-full` | 50 % | Avatars, presence dots |

**Message bubble rule:** `14px 14px 14px 5px` for agent (tail bottom-left), `14px 14px 5px 14px` for user (tail bottom-right).

### 1.5 Elevation

Three levels. At night, shadows go darker and tighter; at day, they stay very soft and green-tinted.

| Level | Day | Night | Use |
|---|---|---|---|
| `elev-0` | none | none | Flat surfaces, list rows |
| `elev-1` (soft) | `0 1px 2px rgba(24,43,35,.05)` | `0 1px 2px rgba(0,0,0,.3)` | Cards at rest, chips |
| `elev-2` (raised) | `0 1px 2px rgba(24,43,35,.06), 0 8px 20px -10px rgba(24,43,35,.18)` | `0 1px 2px rgba(0,0,0,.35), 0 14px 28px -14px rgba(0,0,0,.8)` | Bottom nav bar, FAB, sheets |
| `elev-3` (brand glow) | `0 8px 20px -8px rgba(8,124,69,.65), 0 1px 2px rgba(0,0,0,.2)` | same | Primary CTA, publish button |
| `elev-sheet` | `0 -10px 40px rgba(0,0,0,.35)` | same | Rising sheet |

In Compose, prefer `Modifier.shadow()` with these exact values over M3 `elevation` — M3 tonal elevation would tint the surfaces and break the palette.

### 1.6 Iconography

- **Set:** 46 custom inline SVG symbols (a curated subset of Material Symbols, 24 × 24 viewBox, 2 px stroke, round caps).
- **Sizes used:** 21 px (inline / chip), 22 px (app-bar action), 24 px (row leading icon), 26 px (nav destination).
- **Icon tile:** settings rows use a **38 px rounded square** (radius 11 px) holding a 20 px glyph, tinted `primary-soft` background with `primary-ink` glyph.
- **Touch target:** every tappable icon sits inside a **48 × 48 px** hit area minimum — the visual glyph may be 22 px, the target is 48 px.
- **Accessibility:** every icon button carries `contentDescription`. Decorative icons use `null`. In the mockup 64 explicit `aria-label`s cover the interactive glyphs.

**The 46 icons:** agents, attach, back, battery, bell, bolt, camera, chatpref, chats, check, chevron, clock, db, discover, download, edit, file, filetext, globe, help, image, info, key, lock, logout, mail, menu, mic, moon, more, palette, plus, profile, search, send, settings, shield, signal, spark, star, sun, trash, type, userplus, video, wifi.

Ship these as vector drawables; do not add `material-icons-extended` (it adds ~4 MB for the handful not already needed).

---

## 2. Day mode vs Night mode tokens — side by side

33 semantic tokens. Each resolves differently per mode. **No screen may use a raw hex value — always the token.**

### 2.1 Surfaces & lines

| Token | Day (Light) | Night (Dark) | Role |
|---|---|---|---|
| `canvas` | `#eef2ef` | `#141816` | Page background |
| `surface` | `#ffffff` | `#1d2220` | Cards, rows, app bar |
| `surface-2` | `#f6f8f5` | `#272e2b` | Nested surface, pressed state |
| `surface-3` | `#eef2ef` | `#323b36` | Tertiary / input track |
| `line` | `#dde6e0` | `#3d4842` | Divider, border |
| `line-soft` | `#e9efea` | `#2b3330` | Hairline, list separator |
| `field` | `#eef2ef` | `#272e2b` | Search / text field fill |
| `field-edge` | `#d7e2db` | `#3d4842` | Field stroke |
| `nav-bg` | `rgba(255,255,255,.94)` | `rgba(29,34,32,.95)` | Bottom nav bar (blurred) |
| `scrim` | `rgba(24,43,35,.10)` | `rgba(0,0,0,.45)` | Sheet scrim |

### 2.2 Text & ink

| Token | Day | Night | Contrast (on its surface) |
|---|---|---|---|
| `ink` | `#182b23` | `#f7f9f7` | 14.91:1 / 15.25:1 — AAA |
| `ink-2` | `#4f6157` | `#c3cdc7` | 6.60:1 / 9.89:1 — AA+ |
| `ink-3` | `#5c6d63` | `#93a199` | 5.49:1 / 5.99:1 — AA |

### 2.3 Brand & action

| Token | Day | Night | Notes |
|---|---|---|---|
| `primary` | `#087c45` | `#cbe7d2` | Night flips to brand-soft as the "primary" fill |
| `primary-grad` | `#0b8249 → #087c45` | `#daf0df → #cbe7d2` | 180° top-to-bottom |
| `on-primary` | `#ffffff` | `#17201b` | 5.28:1 / 12.63:1 |
| `primary-soft` | `#ddf3e5` | `#235039` | Tinted container |
| `primary-ink` | `#066a3b` | `#a6e6b9` | Text on `primary-soft` — 5.75:1 / 6.43:1 |
| `mint` | `#42d982` | `#42d982` | **Same both modes** |
| `brand-soft` | `#cbe7d2` | `#183826` | Soft green fill |
| `danger` | `#a4271e` | `#f2b8b5` | 7.27:1 / 9.45:1 |

### 2.4 Chat

| Token | Day | Night | Contrast |
|---|---|---|---|
| `user-bubble` | `#087c45` | `#505752` | white on it: 5.28:1 / 7.02:1 |
| `user-ink` | `#ffffff` | `#f7f9f7` | — |
| `agent-bubble` | `#ffffff` | `#303433` | — |
| `agent-ink` | `#182b23` | `#f7f9f7` | 14.91:1 / 11.92:1 |

### 2.5 Page-level (the mockup chrome, not the app)

| Token | Day | Night |
|---|---|---|
| `page-bg` | `#eef2ef` | `#0d1110` |
| `page-card` | `#ffffff` | `#161b19` |
| `page-ink` | `#182b23` | `#f7f9f7` |
| `page-ink-2` | `#4f6157` | `#a9b8ae` |
| `page-line` | `#dde6e0` | `#2b3330` |
| `halo` | `radial-gradient(900px 420px at 50% -10%, rgba(8,124,69,.10), transparent 70%)` | `…rgba(66,217,130,.10)…` |

### 2.6 Theme engine contract

- **Storage key:** `teamily-theme-mode` (persisted, device-local).
- **Three modes:** `system` | `light` | `dark`. Labels shown to the user: **System**, **Day**, **Night**.
- **System mode** resolves via the OS dark-theme setting; it must also respond to live changes without a restart.
- **Two entry points**, both must agree:
  1. App-bar **sun/moon** toggle — flips between Day and Night only (never sets System).
  2. **Settings → Appearance** — three radio cards (System / Day / Night).
- **Contrast guard:** if the user later adds a custom accent, re-check §6.3 — a teal-on-green accent can drop below 3:1.

---

## 3. The 14 screens

Grouped as the mockup groups them: **Main** (01–05, all carry the bottom nav) and **Settings** (06–14, all carry a detail bar with a back arrow).

---

### 3.01 — Home / Chats

**Purpose:** The default landing screen. A single inbox where agent threads, 1-to-1 chats and group chats coexist.

**Layout structure**
```
┌──────────────────────────────────────┐
│ Status bar (9:41 · signal · wifi · batt) │
├──────────────────────────────────────┤
│ [logo mark]  Teamily AI    [search][menu] │  ← app bar, 60 px
├──────────────────────────────────────┤
│ [🔍 Search chats, agents, people    ] │  ← field, 52 px, pill
├──────────────────────────────────────┤
│ (All)(Unread)(Agents)(Groups)          │  ← chips, 58 × 30
├──────────────────────────────────────┤
│ 4 pinned                               │  ← section label
│ ┌──────────────────────────────────┐ │
│ │ [avatar] Webpage Developer   2m   │ │
│ │          AGENT                   │ │
│ │          Landing page draft is…  │ │
│ ├──────────────────────────────────┤ │
│ │ [avatar] Design Team · 5    12m  │ │
│ │          Alice: the new M3…      │ │
│ └──────────────────────────────────┘ │
│ … 5 more rows …                        │
├──────────────────────────────────────┤
│  [Chats]  Agents  Discover  Profile   │  ← bottom nav, 80 px
└──────────────────────────────────────┘
```

**Components**
- **App bar (60 px):** logo mark 32 px + wordmark, then two 48 × 48 icon buttons (search, overflow). No elevation — flat on `canvas`.
- **Search field (52 px tall, `radius-pill`):** fill `field`, stroke `field-edge`, leading 20 px search glyph, `ink-3` placeholder.
- **Filter chips (58 × 30, `radius-pill`):** four — **All**, **Unread**, **Agents**, **Groups**. Selected chip: `primary` fill, `on-primary` label. Unselected: `surface` fill, `line` stroke, `ink-2` label.
- **Section label:** `4 pinned` — section header style, `ink-3`.
- **Chat row (72 px min height):** leading avatar 48 px; agent rows show a **mint spark badge** bottom-right of the avatar plus a presence dot; title (row title style) + timestamp (`caption`, `ink-3`) on one line; second line = `AGENT` badge (only for agents) + message preview (body, `ink-2`, single line, ellipsis); group rows show member count `Design Team · 5`.
- **Unread state:** title goes `ink` 700, row gets a 3 px `primary` leading edge bar, unread count badge in `primary` fill.
- **Bottom nav (80 px + safe area):** four destinations, active indicator a **58 × 30 pill** in `primary-soft` behind the icon+label.

**States**
| State | Treatment |
|---|---|
| Populated | 7 rows as above (3 agents, 2 people, 2 groups) |
| Empty | Centered: 96 px mint spark glyph in a `primary-soft` circle, headline "No chats yet" (screen title), body "Start a chat with an agent or a person." (body, `ink-2`), primary CTA "Start a chat" |
| Loading | 6 skeleton rows: 48 px circle + two bars (60 % and 40 % width), shimmer 1.4 s, respecting reduce-motion |
| Error | Inline banner under chips: `danger` text + "Retry" text button |

**Navigation**
- Tap row → **Chat (02)**. Groups/people open the same thread screen with a different top bar.
- Tap Agents chip → filtered list, nav stays on Chats.
- Bottom nav → 03 / 04 / 05.
- App-bar search → search mode overlay (inline, no new screen).

---

### 3.02 — Chat

**Purpose:** A single agent (or person/group) thread with inline artifacts.

**Layout structure**
```
┌──────────────────────────────────────┐
│ [←] [avatar] Webpage Developer    [⋮] │  ← top bar, 60 px
│              Agent · always on        │
├──────────────────────────────────────┤
│              Today                    │  ← date divider
│ ┌────────────────────────────────┐  │
│ │ Draft a landing page for our AI │  │  ← user bubble, right, green
│ │ meetup in Palo Alto — dark hero…│  │
│ └────────────────────────────────┘  │
│            9:38 AM                    │
│ ┌──────────────────────────────────┐│
│ │ On it. I'll use the Teamily green ││  ← agent bubble, left, white
│ │ (#087c45) so it stays on-brand…   ││
│ │ ┌────────────────────────────────┐││
│ │ │ Preview  meetup-landing · v1   │││  ← artifact card
│ │ │ Responsive · 12 sections · Lato│││
│ │ │ [Open preview]      [Publish]  │││
│ │ └────────────────────────────────┘││
│ └──────────────────────────────────┘│
│            9:38 AM                    │
│ … 2 more turns …                      │
├──────────────────────────────────────┤
│ [＋]  Message Teamily…        [mic][➤] │  ← composer
└──────────────────────────────────────┘
```

**Components**
- **Top bar (60 px):** back 48 px, agent avatar 40 px, title stack (name row-title 700, subtitle `Agent · always on` caption `ink-3`), overflow 48 px. `surface` fill with a `line-soft` hairline bottom.
- **Date divider:** centered `caption`, `ink-3`, on `canvas`.
- **User bubble:** max width **78 %**, right-aligned, `user-bubble` fill, `user-ink` text, radius `14 14 5 14`, padding 12 × 14.
- **Agent bubble:** max width **85 %**, left-aligned, `agent-bubble` fill, `agent-ink` text, radius `14 14 14 5`, padding 12 × 14.
- **Timestamp:** below each bubble group, `caption`, `ink-3`, aligned to the bubble's outer edge.
- **Inline artifact card:** inside the agent bubble, `radius-lg` 16, `surface-2` fill, `line` stroke. Header: "Preview" overline + filename + version (`meetup-landing · v1`), then a meta line "Responsive · 12 sections · Lato", then **two buttons**: "Open preview" (outlined) and "Publish" (primary, `elev-3` glow).
- **Composer (bottom):** rounded container `radius-3xl` 28, `surface` fill, `line` stroke. Left attach button 44 px; text field grows 1→5 lines; mic 44 px; send 44 × 44 circular `primary` (disabled = `surface-3` fill, `ink-3` icon).

**States**
| State | Treatment |
|---|---|
| Empty | Centered greeting + 3 suggested prompts as `surface-2` chips |
| Streaming | Agent bubble shows a 3-dot typing indicator, then text streams in; a subtle caret at the insert point |
| Thinking | Skeleton bubble with 3 shimmering lines + `Thinking…` label |
| Tool running | Inline status row inside the agent bubble: spinner + "Using web search…" |
| Error | Bubble gets `danger` 1 px border + a "Retry" text button below; the failed bubble keeps its partial text |
| Offline | Composer disabled, banner "You're offline" above it |

**Navigation:** back → **01**; top-bar tap → agent profile sheet; artifact card → external preview / Publish flow.

---

### 3.03 — Agents

**Purpose:** Manage the user's agent roster.

**Layout structure**
```
┌──────────────────────────────────────┐
│ Your agents                     4 active │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ [avatar] Webpage Developer        │ │
│ │ Builds and publishes landing pages│ │
│ │ docs and dashboards from a single │ │
│ │ prompt.                           │ │
│ │ [Working]  [Web]           [Open] │ │
│ └──────────────────────────────────┘ │
│ … 3 more agent cards …                │
│ ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐ │
│ │      + Create a new agent       │ │
│ └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘ │
├──────────────────────────────────────┤
│  Chats  [Agents]  Discover  Profile   │
└──────────────────────────────────────┘
```

**Components**
- **Header:** "Your agents" (screen title) + right-aligned "4 active" (`caption`, `ink-3`).
- **Agent card:** `surface` fill, `radius-lg` 16, `elev-1`, padding 16. Avatar 48 px; name row-title; description body `ink-2` (2 lines max); footer chips — **status** (`Working` = `primary-soft`/`primary-ink`; `Idle` = `surface-2`/`ink-3`) and **category** (`Web` / `Slides` / `Research` / `Finance`) — plus a right-aligned **Open** text button.
- **Create card:** dashed 1.5 px `line` border, `radius-lg`, `surface-2`, centered `primary` "+ Create a new agent".
- **Bottom nav:** active = Agents.

**States:** populated (4) · empty (illustration + "No agents yet" + CTA) · loading (3 skeleton cards) · error (banner + retry).
**Navigation:** Open/row → agent detail sheet; Create → creation flow; nav → 01/04/05.

---

### 3.04 — Discover

**Purpose:** Find and install new agents.

**Layout structure**
```
┌──────────────────────────────────────┐
│ Discover                              │
├──────────────────────────────────────┤
│ ┌──────────────────────────────────┐ │
│ │ FEATURED AGENT                    │ │
│ │ Webpage Developer                 │ │
│ │ Prompt a full landing page and get│ │
│ │ a published link in under two min.│ │
│ │            [Start a chat]         │ │
│ └──────────────────────────────────┘ │
│ Trending in community                 │
│ [av] Slides Assistant     Productivity│
│      Deck in 5 prompts · 2.1k users   │
│ [av] Market Research      Research    │
│      Cited briefs · 1.4k users        │
│ [av] Accounting Assistant Finance     │
│      Invoice triage · 980 users       │
│ [av] Design Critic        Design      │
│      M3 + a11y review · 760 users     │
│ From people you follow                │
│ [av] Alice Chen        12 minutes ago │
│      shared Webpage Developer with…   │
│      · 3 replies                       │
├──────────────────────────────────────┤
│  Chats  Agents  [Discover]  Profile   │
└──────────────────────────────────────┘
```

**Components**
- **Featured card:** hero, `radius-4xl` 32, gradient background (`primary-grad` at low opacity over `surface`), 16 px padding, overline "FEATURED AGENT" (badge style, `primary-ink`), name (screen title), body, primary CTA "Start a chat" with `elev-3` glow.
- **Trending row:** avatar 44 px, name row-title, category chip right-aligned, second line `body` `ink-3` = installs + description.
- **Social row:** avatar + name + relative time (`caption`), then the action sentence, then a `· 3 replies` meta.
- **Section headers:** "Trending in community", "From people you follow" — section style, `ink-3`.

**States:** populated · empty (only the featured card + "Nothing trending yet") · loading rows · error banner.
**Navigation:** featured CTA → **02**; row → agent detail; nav → 01/03/05.

---

### 3.05 — Profile

**Purpose:** Identity, quick actions and the entry point to all settings.

**Layout structure**
```
┌──────────────────────────────────────┐
│  [avatar 80]                          │
│  Fareed Khan          ← display 900/21│
│  fareed.khan@gmail.com                │
│  [Teamily Pro]  Member since Mar 2024 │
│           [Edit profile]              │
├──────────────────────────────────────┤
│ Chats  Agents  Artifacts              │  ← 3-up stat cards
│  128     4       37                   │
├──────────────────────────────────────┤
│ Quick actions                         │
│  Add an agent                4 active │
│  Appearance                Night mode │
│  Storage              3.2 GB of 15 GB │
│  Notifications              5 of 7 on │
├──────────────────────────────────────┤
│ Account                               │
│  Personal information  Name, email, avatar │
│  Privacy & security    Passcode, blocked…  │
│  Subscription          Pro · renews 12 Nov │
│  All settings                       ›  │
│  Sign out                              │
├──────────────────────────────────────┤
│  Chats  Agents  Discover  [Profile]   │
└──────────────────────────────────────┘
```

**Components**
- **Header block:** avatar 80 px (tap → photo picker), display name, email (`body`, `ink-2`), a **Pro pill** (`primary-soft` fill, `primary-ink` label, `radius-pill`) and "Member since Mar 2024" (`caption`), then an outlined **Edit profile** button.
- **3-up stat cards:** equal width, `surface` `radius-lg` `elev-1`, value in row-title 700, label in `caption` `ink-3`.
- **Settings rows (72 px):** leading 38 px icon tile, title row-title, optional trailing value (`body`, `ink-3`) or chevron, hairline separator inset to the text start.
- **Sign out row:** title in `danger`, no chevron.
- **Confirmation dialog (shown in-memory in the mockup):** title "Sign out of Teamily AI?", body "Your chats and artifacts stay on your account. You'll need to sign in again on this phone.", actions **Sign out** (`danger` filled) / **Cancel** (text).
- **Bottom nav:** active = Profile.

**States:** populated · loading (avatar+name skeleton) · error (profile fetch failed → retry card).
**Navigation:** Edit profile → 08; Appearance → 07; Storage → 12; Notifications → 09; Personal information → 08; Privacy & security → 10; Subscription → billing (out of scope); All settings → 06; Sign out → dialog → auth.

---

### 3.06 — Settings

**Purpose:** Every setting, grouped.

**Structure & groups (top → bottom)**
| Group | Rows | Trailing value |
|---|---|---|
| **Account** | Fareed Khan (name + email) · Subscription | Pro plan · 15 GB storage / Renews 12 Nov |
| **Look & feel** | Appearance · Text size · Language | Night / Default / English (US) |
| **Messages** | Notifications · Chat preferences | 5 of 7 on / Send with Enter, typing indicators |
| **Privacy & security** | App lock, blocked users, data | — |
| **Data** | Storage · Media auto-download | 3.2 GB used of 15 GB / Wi-Fi only |
| **Support** | Help centre · About Teamily AI | — / Version, terms, licenses |
| — | **Sign out** | (danger) |

**Components**
- **Detail bar (60 px):** back arrow + "Settings" (screen title). No right action.
- **Group label:** uppercase section style, `ink-3`, 16 px left inset, 8 px above / 6 px below.
- **Group container:** `surface` fill, `radius-xl` 20, `elev-1`, rows separated by `line-soft` hairlines inset to the text start.
- **Row height:** 60 px when title-only, 72 px when it has a subtitle. Icon tile 38 px.
- **Footer:** centered `caption` `ink-3` — "Teamily AI 1.8.2 (build 1820) · Made with Lato".

**States:** static (no loading/empty needed — reads local + cached prefs) · error per row (value shows "—" and row is tappable to retry).
**Navigation:** each row → its screen (07–13); Sign out → dialog.

---

### 3.07 — Appearance / Theme

**Purpose:** The only place all three theme modes are selectable, plus a live preview.

**Structure**
```
Appearance
  Theme
  ┌──────────────────────────────────┐
  │ ( ) System   Auto      Follows your│
  │             Android theme setting… │
  │ (•) Night    Dark      Deep charcoal│
  │                        canvas #141816│
  │ ( ) Day      Light     Bright canvas│
  │                        #eef2ef…     │
  └──────────────────────────────────┘
  Live preview
  ┌──────────────────────────────────┐
  │ [avatar] Webpage Developer         │
  │          Preview of the current…   │
  │  ┌──────────────────────────────┐ │
  │  │ Draft is ready — shall I …    │ │
  │  │          [Yes, publish it.]    │ │
  │  └──────────────────────────────┘ │
  │  [＋] Message Teamily…      [➤]    │
  └──────────────────────────────────┘
  Text size
  (Small) (• Default) (Large)
  Changing the theme applies instantly across every screen and is remembered on this device.
```

**Components**
- **Theme radio cards (three, full width, `radius-xl`, padding 16):** radio 20 px, bold label (`System`/`Night`/`Day`), a short mode chip (`Auto`/`Dark`/`Light`), and a description containing the actual hex. Selected card: `primary-soft` fill + `primary` 1.5 px border.
- **Live preview:** a miniature chat (agent bubble + user bubble + composer) rendered in the currently selected theme — must update **without leaving the screen**.
- **Text-size segmented control:** three segments, `radius-pill`, selected = `primary` fill.
- **Footer note:** `caption` `ink-2`.

**States:** three selections; changing re-paints the whole app instantly and writes `teamily-theme-mode`.
**Navigation:** back → 06.

---

### 3.08 — Edit profile

**Purpose:** Edit display name, photo, handle and bio.

**Components**
- **Detail bar:** back + "Edit profile", right action **Save** (primary text button, disabled until dirty).
- **Profile photo:** 96 px circle, camera badge 32 px bottom-right, caption "Tap a face below or upload".
- **Fields** (each: label above, 52 px input, `radius-md` 14, `field` fill, `field-edge` stroke, helper text `caption` `ink-3`):
  - **Full name** — "Shown to people and agents you chat with."
  - **Email** — "Used for sign-in and receipts. We'll send a code to confirm a change."
  - **Teamily ID** — "Your handle is unique — 30 days between changes."
  - **About you** — multiline 3–4 lines, character counter.
- **Actions:** primary "Save changes" (full width, `elev-3`), text "Cancel".

**States:** clean (Save disabled, `ink-3`) · dirty (Save enabled) · saving (spinner in Save) · validation error (field border → `danger`, helper text `danger`) · server error (banner at top).
**Navigation:** Save → back to 05 with optimistic update; Cancel → discard-confirm if dirty.

---

### 3.09 — Notifications

**Purpose:** Push/email preferences and quiet hours.

**Structure**
| Group | Rows | Type |
|---|---|---|
| **Push** | Push notifications (Master switch for this device) | Switch |
| | Direct messages (People and group chats) | Switch |
| | Agent replies (When a running agent finishes a task) | Switch |
| | Agent suggestions (Proactive ideas in chat) | Switch |
| | Community activity (Installs, ratings and mentions) | Switch |
| **Email** | Weekly digest (Chats, artifacts and agent usage) | Switch |
| | Product news (New agents and features) | Switch |
| **Behaviour** | Quiet hours (Silence everything overnight) — `22:00 – 07:00` | Switch + time range |
| | Sound (Teamily chime) | Switch |

**Components**
- **Switch:** M3 switch, track 52 × 32, **hit target 56 × 48**. On = `primary` track; off = `surface-3` track with `line` outline.
- **Master-off cascade:** turning "Push notifications" off visually disables (40 % opacity, non-interactive) all child switches but **remembers their individual values** — re-enabling restores them.
- **Quiet hours row:** when on, shows the time range inline; tapping it opens a time-range picker sheet.
- **Footer note:** `caption` `ink-2` — "Turn off agent replies and you'll only see finished tasks in the chat list."

**States:** defaults as shown ("5 of 7 on") · permission-denied banner if OS notification permission is missing, with a "Open settings" action.
**Navigation:** back → 06.

---

### 3.10 — Privacy & security

**Purpose:** App lock, 2FA, privacy toggles, blocked users, data controls.

**Structure**
| Group | Rows | Type |
|---|---|---|
| **Security** | App lock (Fingerprint or PIN to open Teamily) | Switch |
| | Change passcode (6-digit PIN) | Nav (only when App lock on) |
| | Two-step verification (Code required at new sign-ins) | Switch |
| **Privacy** | Read receipts (Let others see when you've read) | Switch |
| | Share chat history with agents (Agents can use earlier context) | Switch |
| | Help improve Teamily (Share anonymised diagnostics) | Switch |
| **Blocked users** | David Park — "Blocked 3 October 2026" | Row + **Unblock** |
| | +1 415 ··· 8802 — "Unknown contact · blocked 18 Sept 2026" | Row + **Unblock** |
| **Your data** | Download my data (Chats, artifacts and profile) | Nav |
| | Clear chat history (Keeps your agents and artifacts) | Nav, `danger` |

**Components**
- **Switch** as §3.09.
- **Blocked-user row:** 44 px avatar (or phone glyph for unknown contacts), name, `caption` `ink-3` block date, right-aligned **Unblock** outlined button.
- **Destructive row:** title `danger`; tapping opens a confirmation sheet with explicit consequences.
- **Conditional row:** "Change passcode" only renders when App lock is on — the layout must not reserve its space.

**States:** blocked list empty → "No blocked users" (`ink-3`, centered, 64 px vertical padding) · destructive confirm sheet · error banner.
**Navigation:** back → 06; Download my data → progress sheet; Clear chat history → confirm → toast.

---

### 3.11 — Chat preferences

**Purpose:** Composer and thread behaviour.

**Structure**
| Group | Rows | Type |
|---|---|---|
| **Composer** | Send with Enter (Off means Enter adds a new line) | Switch |
| | Hold to record voice (Mic button records instead of dictating) | Switch |
| | Preview before sending (Confirm links and files first) | Switch |
| **Thread** | Typing indicators (Show when an agent is responding) | Switch |
| | Auto-play media (GIFs and video previews in chat) | Switch |
| | Agent activity cards (Inline artifact previews in replies) | Switch |
| **Default for new chats** | Default agent (`Webpage Developer`) | Nav |
| | Auto-archive chats (`After 30 days`) | Nav |

**Components:** same row grammar. Trailing values in `body` `ink-3` where a choice is shown.
**States:** static · error banner. Note "Send with Enter" is the only row whose **off** state changes composer behaviour — document it in the row subtitle (it does).
**Navigation:** back → 06.

---

### 3.12 — Storage

**Purpose:** Show usage and free space.

**Structure**
```
Storage
      3.2 GB
   of 15 GB used            ← hero
  ┌──────────────────────────────────┐
  │████████░░░░░░░░░░░░░░░░░░░░░░░░░░│  ← progress bar
  └──────────────────────────────────┘
  Chat media                 1.1 GB
  Artifacts & files          1.4 GB
  Cached previews            0.5 GB
  Agent logs                 0.2 GB
  Clean up
   Cached previews   Safe to clear — regenerated on demand   0.5 GB
   Old chat media    Photos and video older than 90 days     0.4 GB
   Agent logs        Run history used for debugging           0.2 GB
   Artifact library  All artifacts          37 files · 4 agents
  Storage is shared with the Pro plan. Deleting artifacts here
  removes them from every device.
```

**Components**
- **Hero:** used amount in display 900, "of 15 GB used" in `body` `ink-2`.
- **Progress bar:** 8 px tall, `radius-pill`, `surface-3` track, `primary` fill. Turns `danger` above 90 %.
- **Breakdown rows:** label (row title) + value right-aligned (body `ink-2`); no chevrons (read-only).
- **Clean-up rows:** tappable, with subtitle + size, trailing chevron; each opens a confirm sheet.
- **Warning footnote:** `caption` `ink-2` — the "removes them from every device" line is not optional; it is a destructive-action disclosure.

**States:** loading (skeleton hero + 4 rows) · populated · near-full warning (>90 %, `danger` bar) · cleaning (row shows inline spinner, disabled).
**Navigation:** back → 06; Artifact library → artifacts screen.

---

### 3.13 — Language

**Purpose:** App language and agent reply language.

**Structure**
| Group | Rows |
|---|---|
| **App language** | English (US) · English (UK) · हिन्दी Hindi · Español Spanish · 日本語 Japanese · العربية Arabic |
| **Agent replies** | Always reply in app language — "Agents will match your language choice" (Switch) |
| | Agent language — `Auto-detect` (Nav) |

**Components**
- **Language row:** native name (row title) + English name (`caption` `ink-3`) + radio 20 px, selected = `primary` ring. 64 px tall.
- **RTL:** selecting **العربية** must flip the whole layout to RTL — the mockup is LTR-only, so verify inset/chevron mirroring during implementation.
- **Footer note:** `caption` `ink-2` — "Some agents and community posts may still appear in their original language."

**States:** current selection (persisted) · applying (brief spinner, then the app re-composes in the new locale).
**Navigation:** back → 06.

---

### 3.14 — About

**Purpose:** Version, legal, support.

**Structure**
```
About
  Version 1.8.2 · build 1820
  A messenger where people and AI agents work in the same threads.
  Legal
   Terms of service          Effective 1 August 2026
   Privacy policy            How your chats are stored and processed
   Open-source licenses      Lato, Material 3 and 42 libraries
  Support
   Contact support           support@teamily.ai
   Check for updates         Last checked today
  Teamily AI · made with Lato
  © 2026 Teamily Labs. All rights reserved.
  Package aiteamily.android · 1.8.2 (1820)
```

**Components:** app mark 72 px, version line (`body` `ink-2`), tagline, then two groups using the standard row grammar, then a centered footer (`caption` `ink-3`) in three lines.
**States:** "Check for updates" → checking (inline spinner) → "Up to date" or "Update available" chip.
**Navigation:** back → 06; legal rows → in-app web view (`allowFileAccess=false`, `allowContentAccess=false`, `blockNetworkLoads` only for the deep-link case — see the CodePreview note in §7).

---

## 4. Navigation

### 4.1 Bottom navigation

- **4 destinations:** **Chats** (icon `chats`) · **Agents** (`agents`) · **Discover** (`discover`) · **Profile** (`profile`).
- **Bar height:** 80 px + system inset. Background `nav-bg` with a backdrop blur; top hairline `line-soft`.
- **Item:** icon 26 px over label (`chip label` style 12 px/700). **Each item's hit target ≥ 48 px** — the mockup gives each a full-width quarter of the bar, so ~97 px wide × 80 px tall at 390 px.
- **Active indicator:** an M3 **58 × 30 px pill** in `primary-soft` behind the icon; icon and label switch to `primary-ink`.
- **Present on:** screens **01, 03, 04, 05** only — **4 of 14**, verified by counting `navbar` blocks in the mockup.
- **Not present on:** screens 02 and 06–14 — those use a back-arrow detail bar. Screen 02 has its own thread top bar with back.
- **⚠ Mockup-copy discrepancy:** the mockup's own design-system caption says the nav is *"present on all 5 main screens"*. It is not — screen 02 (Chat) has no bottom nav. **Build 4 screens**, not 5. This is the only place where the mockup's prose and its markup disagree.

### 4.2 Screen-to-screen flow

```
                        ┌──────────────────────┐
                        │  01 Home / Chats     │
                        └───┬──────────────┬───┘
              tap chat row  │              │ bottom nav
                            ▼              │
                        ┌────────────┐     │
                        │  02 Chat   │     │
                        └────────────┘     │
                                           │
   ┌───────────────────────────────────────┴─────────────┐
   │                                                     │
   ▼                       ▼                             ▼
┌──────────┐         ┌───────────┐               ┌──────────────┐
│ 03 Agents│         │04 Discover│               │ 05 Profile   │
└──────────┘         └───────────┘               └──┬───┬───┬───┘
                                                    │   │   │
                             ┌──────────────────────┘   │   └──────────────┐
                             ▼                          ▼                  ▼
                        ┌─────────┐              ┌────────────┐    ┌──────────────┐
                        │08 Edit  │              │07 Appear-  │    │10 Privacy &  │
                        │ profile │              │   ance     │    │   security   │
                        └─────────┘              └────────────┘    └──────────────┘
                             ▲                          ▲                  ▲
                             │                          │                  │
                        ┌────┴──────────────────────────┴──────────────────┴───┐
                        │                    06 Settings                        │
                        └──┬──────────┬──────────┬──────────┬──────────┬────────┘
                           ▼          ▼          ▼          ▼          ▼
                      ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
                      │09 Noti-│ │11 Chat │ │12 Stor-│ │13 Lang-│ │14 About│
                      │fication│ │  prefs │ │  age   │ │  uage  │ │        │
                      └────────┘ └────────┘ └────────┘ └────────┘ └────────┘
```

**Rules**
1. **Only 4 screens carry the bottom nav.** Everything else is pushed with a back arrow.
2. **Every detail screen has a working back arrow** — Android back gesture must do the same thing.
3. **Profile is the only route to Settings.** There is no settings icon in the Home app bar (it has search + overflow only).
4. **Theme can be changed from two places** (07 and the app bar sun/moon on main screens) — they must share one resolver.
5. **Deep-link rule:** a notification or link into any of 06–14 must synthesise a back stack of `01 → 05 → 06 → target` so back behaves sanely.

### 4.3 Transitions

| Transition | Spec |
|---|---|
| Bottom-nav switch | No slide — cross-fade 120 ms, state preserved per tab |
| Push detail (01→02, 05→06→07) | Shared-axis X slide: enter from right 100 %, 220 ms, `FastOutSlowIn`; exit to 30 % with scrim |
| Pop | Reverse, 200 ms |
| Sheet | Slide up 260 ms `FastOutSlowIn`, scrim `scrim` fading in |
| Theme change | No animation on colours (instant), but a 150 ms cross-fade on the whole tree is acceptable if it doesn't stutter |
| **Reduce motion** | All of the above collapse to a 0 ms swap; **keep** the theme cross-fade off too |

---

## 5. Material 3 mapping (Compose)

### 5.1 `ColorScheme`

```kotlin
// Day
private val DayColorScheme = lightColorScheme(
    primary            = Color(0xFF087C45),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFDDF3E5),
    onPrimaryContainer = Color(0xFF066A3B),
    secondary          = Color(0xFF42D982),   // logo mint
    onSecondary        = Color(0xFF17201B),
    secondaryContainer = Color(0xFFCBE7D2),   // brand soft
    onSecondaryContainer= Color(0xFF066A3B),
    tertiary           = Color(0xFF066A3B),
    onTertiary         = Color(0xFFFFFFFF),
    background         = Color(0xFFEEF2EF),   // light canvas
    onBackground       = Color(0xFF182B23),   // ink
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF182B23),
    surfaceVariant     = Color(0xFFF6F8F5),
    onSurfaceVariant   = Color(0xFF4F6157),   // ink-2
    surfaceContainerLowest  = Color(0xFFFFFFFF),
    surfaceContainerLow     = Color(0xFFF6F8F5),
    surfaceContainer        = Color(0xFFFFFFFF),
    surfaceContainerHigh    = Color(0xFFEEF2EF),
    surfaceContainerHighest = Color(0xFFEEF2EF),
    outline            = Color(0xFFDDE6E0),   // line
    outlineVariant     = Color(0xFFE9EFEA),   // line-soft
    error              = Color(0xFFA4271E),   // danger
    onError            = Color(0xFFFFFFFF),
    errorContainer     = Color(0xFFFBE9E7),
    onErrorContainer   = Color(0xFFA4271E),
    scrim              = Color(0x1A182B23),   // 10 %
    inverseSurface     = Color(0xFF141816),
    inverseOnSurface   = Color(0xFFF7F9F7),
    inversePrimary     = Color(0xFFCBE7D2),
)

// Night
private val NightColorScheme = darkColorScheme(
    primary            = Color(0xFFCBE7D2),
    onPrimary          = Color(0xFF17201B),
    primaryContainer   = Color(0xFF235039),
    onPrimaryContainer = Color(0xFFA6E6B9),
    secondary          = Color(0xFF42D982),
    onSecondary        = Color(0xFF17201B),
    secondaryContainer = Color(0xFF183826),
    onSecondaryContainer=Color(0xFFA6E6B9),
    tertiary           = Color(0xFFA6E6B9),
    onTertiary         = Color(0xFF17201B),
    background         = Color(0xFF141816),   // dark canvas
    onBackground       = Color(0xFFF7F9F7),
    surface            = Color(0xFF1D2220),
    onSurface          = Color(0xFFF7F9F7),
    surfaceVariant     = Color(0xFF272E2B),
    onSurfaceVariant   = Color(0xFFC3CDC7),
    surfaceContainerLowest  = Color(0xFF141816),
    surfaceContainerLow     = Color(0xFF1D2220),
    surfaceContainer        = Color(0xFF1D2220),
    surfaceContainerHigh    = Color(0xFF272E2B),
    surfaceContainerHighest = Color(0xFF323B36),
    outline            = Color(0xFF3D4842),
    outlineVariant     = Color(0xFF2B3330),
    error              = Color(0xFFF2B8B5),
    onError            = Color(0xFF50120F),
    errorContainer     = Color(0xFF3D1512),
    onErrorContainer   = Color(0xFFF2B8B5),
    scrim              = Color(0x73000000),   // 45 %
    inverseSurface     = Color(0xFFEEF2EF),
    inverseOnSurface   = Color(0xFF182B23),
    inversePrimary     = Color(0xFF087C45),
)
```

**⚠ Two `ColorScheme` slots cannot carry this design and need a side channel:**

1. **`user-bubble`** — day `#087C45`, night `#505752`. In day mode it equals `primary`; at night it does **not**. Do not map it to `primary`; use a custom token (see §5.4).
2. **`agent-bubble`** — day `#FFFFFF`, night `#303433`. Day equals `surface`; night does not. Same solution.

Everything else maps cleanly.

### 5.2 `Typography`

```kotlin
private val DuaTypography = Typography(
    displayLarge   = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Black,   fontSize = 21.sp, lineHeight = 25.sp, letterSpacing = (-0.02).em),
    titleLarge     = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold,    fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.01).em),
    titleMedium    = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold,    fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    bodyMedium     = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal,  fontSize = 14.5.sp, lineHeight = 22.5.sp, letterSpacing = 0.em),
    bodySmall      = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal,  fontSize = 12.5.sp, lineHeight = 18.sp, letterSpacing = 0.em),
    labelLarge     = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold,    fontSize = 12.sp, lineHeight = 12.sp, letterSpacing = 0.02.em),
    labelMedium    = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Bold,    fontSize = 10.sp, lineHeight = 10.sp, letterSpacing = 0.10.em),
    labelSmall     = TextStyle(fontFamily = Lato, fontWeight = FontWeight.Normal,  fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.01.em),
    // section header (uppercase labels) — no M3 slot, use a custom token
    // headlineSmall  = TextStyle(... 13.sp, letterSpacing = 0.06.em)
)
```

Note `14.5.sp` and `12.5.sp` are intentional — they are the mockup's values. If the team prefers integers, `14.sp`/`15.sp` and `12.sp`/`13.sp` are acceptable, but then bubble line lengths shift slightly.

### 5.3 `Shapes`

```kotlin
private val DuaShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small      = RoundedCornerShape(11.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
    // not in M3: bubble tails (14/5), sheets (26 top), hero (32), pill (999)
)
```

**Not representable in `Shapes`** — keep as tokens: bubble `14dp 14dp 14dp 5dp` / `14dp 14dp 5dp 14dp`, sheet top `26dp`, hero `32dp`, pill `CircleShape`.

### 5.4 Custom tokens (the `DuaTokens` object)

Because M3 has no slot for these, expose a `CompositionLocal`:

| Token | Day | Night |
|---|---|---|
| `userBubble` | `#087C45` | `#505752` |
| `userBubbleInk` | `#FFFFFF` | `#F7F9F7` |
| `agentBubble` | `#FFFFFF` | `#303433` |
| `agentBubbleInk` | `#182B23` | `#F7F9F7` |
| `ink2` | `#4F6157` | `#C3CDC7` |
| `ink3` | `#5C6D63` | `#93A199` |
| `field` | `#EEF2EF` | `#272E2B` |
| `fieldEdge` | `#D7E2DB` | `#3D4842` |
| `navBg` | `rgba(255,255,255,.94)` | `rgba(29,34,32,.95)` |
| `primaryGrad` | `#0B8249 → #087C45` | `#DAF0DF → #CBE7D2` |
| `mint` | `#42D982` | `#42D982` |
| `brandSoft` | `#CBE7D2` | `#183826` |
| `elevBrand` | `0 8px 20px -8px rgba(8,124,69,.65)` | same |

### 5.5 Component mapping

| UI element | M3 component | Notes |
|---|---|---|
| Bottom nav | `NavigationBar` | Override height to 80 dp; custom 58×30 pill indicator |
| Filter chips | `FilterChip` | Height 30 dp (below M3 default 32) — override |
| Search field | `OutlinedTextField` | 52 dp height, `CircleShape` |
| Message bubble | Custom `Surface` | Not `Card` — radius and tail are bespoke |
| Settings row | `ListItem` or custom `Row` | 72 dp min, 38 dp icon tile |
| Switch | `Switch` (M3) | Track 52×32, target 56×48 |
| Sheet | `ModalBottomSheet` | Top radius 26 dp, drag handle 32×4 |
| Progress bar | `LinearProgressIndicator` | 8 dp, `CircleShape` |
| Radio card | `RadioButton` + `Surface` | Selected = `primary-soft` + 1.5 dp `primary` border |
| FAB | `FloatingActionButton` | 56 dp, squircle radius 18 dp |
| Buttons | `Button` / `OutlinedButton` / `TextButton` | Height 48 dp, radius 14 dp, label 15 sp/700 |

---

## 6. Accessibility

### 6.1 Touch targets — ≥ 48 px, everywhere

| Element | Visual size | Hit target |
|---|---|---|
| Bottom-nav item | icon 26 | **≥ 48** (≈97 × 80) |
| App-bar icon button | glyph 22 | **48 × 48** |
| Settings row | 60–72 tall | full-row width × height |
| Switch | 52 × 32 track | **56 × 48** |
| Chip | 58 × 30 | **≥ 48 tall** (vertical padding extends the target) |
| Composer send / mic | 44 circle | **48 × 48** |
| Chat row | 72 tall | full row |
| List chevron / radio | 20–24 | row carries the target |

The mockup states this explicitly: *"every row ≥ 72px tall"* and *"one 48px+ target per item"*. Do not shrink any target to match a visual.

### 6.2 Contrast — measured, not assumed

The mockup claims "AA on all text pairs". Verified by computing WCAG 2.1 relative luminance for the real hex values:

| Pair | Day | Night | Result |
|---|---|---|---|
| `ink` / `surface` | 14.91:1 | 15.25:1 | AAA |
| `ink` / `canvas` | 13.19:1 | 16.94:1 | AAA |
| `ink-2` / `surface` | 6.60:1 | 9.89:1 | AA+ |
| `ink-3` / `surface` | 5.49:1 | 5.99:1 | AA |
| `ink-3` / `canvas` | 4.86:1 | 6.65:1 | AA |
| `primary` / `surface` | 5.28:1 | 12.21:1 | AA / AAA |
| `on-primary` / `primary` | 5.28:1 | 12.63:1 | AA / AAA |
| `primary-ink` / `primary-soft` | 5.75:1 | 6.43:1 | AA |
| `user-ink` / `user-bubble` | 5.28:1 | 7.02:1 | AA / AAA |
| `agent-ink` / `agent-bubble` | 14.91:1 | 11.92:1 | AAA |
| `danger` / `surface` | 7.27:1 | 9.45:1 | AAA |

**All text pairs pass AA in both modes.** ✅

### 6.3 The one failure — mint as text

| Pair | Day | Night |
|---|---|---|
| `mint` `#42D982` as **text** on `canvas` | **1.62:1 — FAIL** | 9.79:1 — AAA |

Mint is a **fill and accent colour only** — the logo, presence dots, the active spark, decorative strokes. It must **never** carry text or a meaningful icon on a light background. At night it is fine (9.79:1).

*Rule for the developer:* if mint must convey information on light, put it in a filled shape with `ink` on top, or swap to `primary-ink` `#066A3B` (5.75:1).

### 6.4 Other accessibility requirements

- **Content descriptions:** every icon-only button needs one (mockup has 64 `aria-label`s → `contentDescription`). Decorative icons → `null`. Agent avatars: `contentDescription = "<Agent name>"`.
- **Reduce motion:** honour `Settings.Global.ANIMATOR_DURATION_SCALE` / Compose's motion-duration scale. All transitions in §4.3 collapse to 0 ms; skeleton shimmer becomes a static 20 % overlay.
- **TalkBack order:** app bar → content → bottom nav. Chat bubbles read sender, time, then text. Artifact cards announce "Preview, meetup-landing version 1, responsive, 12 sections".
- **Font scaling:** layout must survive 2× font scale. Rows grow vertically (do not truncate titles); the bottom nav label may hide at ≥1.5× rather than clip.
- **Focus order:** the theme radio cards are a single radio group; the text-size control is a segmented group with proper `selectableGroup`.
- **Live regions:** streaming agent text uses a polite live region so TalkBack reads new tokens without re-reading the whole bubble.
- **Destructive actions** (Sign out, Clear chat history, Delete artifact) always confirm, and the confirm button is the non-primary position for the destructive one.
- **Colour is never the only signal:** unread state also changes weight + adds an edge bar; "Working" vs "Idle" uses text, not just tint.

---

## 7. Implementation notes & guardrails

1. **No raw hex in screens.** Every colour resolves through `MaterialTheme.colorScheme` or `DuaTokens`. A `Color(0x…)` outside `ui/theme/` is a bug.
2. **Two-source theme** — the app-bar sun/moon and the Appearance radios must call the same resolver; a mismatch between them is a visible bug (this exact class of bug has appeared before).
3. **Lato must be subset.** Full family ≈ 4.8 MB; subset ≈ 180 KB for the 6 shipped scripts. Bundle only 400/700/900.
4. **Do not pull in `material-icons-extended`** (~4 MB). The design needs 46 glyphs; ship them as vectors.
5. **`CodePreview` WebView:** if this design's artifact preview uses a WebView, it must keep `allowFileAccess=false`, `allowContentAccess=false`, `blockNetworkLoads=true`, and must not expose a JS bridge to untrusted preview content.
6. **Keep `DuaCalligraphyLogo`** in the chat content — it is the brand mark on the thread screen; do not replace it with the generic wordmark.
7. **Voice orb:** if the voice surface is built from this system, keep the Gold80 orb and the `#8B7CF6` speaking glow — that accent is outside this palette by design.
8. **⚠ Not specified by the mockup:** dark-mode `meta theme-color` is `#0d1110` and light is `#eef2ef` — set the status-bar colour to those, with light status-bar icons at night.

---

## 7A. Icon sheet — the 46 glyphs

Every glyph the design needs, with its canonical role. Names are the mockup's own symbol IDs (`i-*` → `ic_*` as Android vector drawables). 24 × 24 viewBox, 2 px stroke, round caps, `fill = none` except where noted.

| Glyph | Role | Glyph | Role |
|---|---|---|---|
| `ic_agents` | Agents nav, agent chip | `ic_mail` | Email field, email group |
| `ic_attach` | Composer attach (＋) | `ic_menu` | Home app-bar overflow |
| `ic_back` | Detail-bar back arrow | `ic_mic` | Composer mic / hold-to-record |
| `ic_batt` | Status bar battery | `ic_moon` | Night-mode toggle |
| `ic_bell` | Notifications row + group | `ic_more` | Vertical-dots overflow |
| `ic_bolt` | Agent "Working" status | `ic_palette` | Appearance row |
| `ic_camera` | Profile-photo badge | `ic_plus` | Create agent, add |
| `ic_chatpref` | Chat preferences row | `ic_profile` | Profile nav, personal info |
| `ic_chats` | Chats nav | `ic_search` | App-bar search, search field |
| `ic_check` | Selected / success | `ic_send` | Composer send (filled) |
| `ic_chev` | Row disclosure chevron | `ic_settings` | Settings entry |
| `ic_clock` | Relative time, quiet hours | `ic_shield` | Privacy & security |
| `ic_db` | Storage row | `ic_signal` | Status bar signal |
| `ic_discover` | Discover nav | `ic_spark` | Agent spark badge, empty state |
| `ic_download` | Download my data | `ic_star` | Rating, featured |
| `ic_edit` | Edit profile | `ic_sun` | Day-mode toggle |
| `ic_file` | Artifact / file row | `ic_trash` | Clear history, delete |
| `ic_filetext` | Text artifact | `ic_type` | Text size row |
| `ic_globe` | Language, browser tool | `ic_userplus` | Add an agent, invite |
| `ic_help` | Help centre | `ic_video` | Media / video row |
| `ic_image` | Profile photo, image artifact | `ic_wifi` | Media auto-download, status |
| `ic_info` | About row | | |
| `ic_key` | Passcode / 2FA | | |
| `ic_lock` | App lock | | |
| `ic_logout` | Sign out | | |

**Filled variants needed:** `ic_send`, `ic_star`, `ic_check` (selected states), and `ic_spark` — export each as a separate drawable (`ic_send_filled` etc.). Do not rely on `tint` to fake fill.

---

## 8. Quick reference card

```
CANVAS       day #eef2ef   night #141816
SURFACE      day #ffffff   night #1d2220
INK          day #182b23   night #f7f9f7
INK-2        day #4f6157   night #c3cdc7
INK-3        day #5c6d63   night #93a199
PRIMARY      day #087c45   night #cbe7d2
ON-PRIMARY   day #ffffff   night #17201b
USER BUBBLE  day #087c45   night #505752
AGENT BUBBLE day #ffffff   night #303433
MINT         #42d982 (both)  ← fill only, never text on light
DANGER       day #a4271e   night #f2b8b5

TYPE   Lato 400 / 700 / 900.  21 display · 17 title · 15 row · 14.5 body
RADIUS 5 · 11 · 14 · 16 · 20 · 26 · 28 · 32 · 999
SPACE  base 4 — gutter 16, row 72, nav 80, chip 58×30, switch 56×48
TOUCH  ≥ 48 dp everywhere
NAV    Chats · Agents · Discover · Profile  (on screens 01, 03, 04, 05 only)
STATES every screen needs: default · empty · loading · error
```

---

*End of specification — 14 screens, 33 semantic tokens, 9 type roles, verified contrast.*
