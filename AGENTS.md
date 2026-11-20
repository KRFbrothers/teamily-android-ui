# AGENTS.md — Dua Gen AI Engineering & Agent Rulebook

> **Ye file kis ke liye hai:** Koi bhi AI agent (ya naya developer) jo is Compose + Cloud AI project me kaam kare.
> **Maksad:** Zero unverified assumptions, zero unpermitted code edits, enterprise-grade Android quality, aur seamless multi-agent tool execution.
> **Scope:** `teamily-android-ui` (Jetpack Compose + Material 3, Kotlin, single-module `app`) & `dua-backend` integration.
> **Last updated:** 2026-11-21

---

## 0. Sabse Pehle — 5 Golden Rules

Agar aap sirf 5 cheezein yaad rakhein, to ye:

1. **Pehle padho, phir likho.** File me jo sach me hai wahi batao — memory, guess ya "generally aisa hota hai" se nahi.
2. **Assumption zero — missing cheez maango.** Kuch ambiguous ya missing hai to guess mat karo. Stop karke 4 clear options + 1 recommendation do simple Hinglish me.
3. **Bina explicit permission code change mat karo.** Audit/report maangi hai to sirf report do. Fix/modify tabhi karo jab explicitly bola gaya ho.
4. **Existing files me kaam karo aur purana untouched rakho.** Zero duplicate files, zero parallel classes. Version control me clean feature/fix branches use karo.
5. **Jo verify nahi kar paye, wo saaf likho.** Chhupao mat — report ke aakhir me "Jo verify nahi kar paya" section mandatory hai.

---

## 1. Project Overview & Architecture

**Dua Gen AI** ek production-grade, enterprise-level Android AI assistant application hai jo Kotlin, Jetpack Compose, aur Material 3 par bani hai, backed by a cloud-native AI & API platform.

- **Android Client (`teamily-android-ui`):** Native Android app featuring Material 3 Forest-Slate design system, Lato typography, a 5-tab bottom navigation dock (`Chats`, `Agents`, `Discover`, `Memory`, `Artifacts`), two pinned agents (`Personal Agent` in fast `auto` mode and `Dua Agent` in `agent` mode with 5 specialized skills), aur navigation through `DuaGenAINavHost`.
- **Cloud Backend (`dua-backend`):** Separated backend repository running a Node.js API gateway (`dua-api` on Google Cloud Run), a Python Flask/ADK AI execution engine (`dua-ai-engine` on Google Cloud Run), aur Supabase PostgreSQL database with vector memory support.
- **Live Cloud Run URLs:**
  - `dua-api`: `https://dua-api-228305912525.us-central1.run.app`
  - `dua-ai-engine`: `https://dua-ai-engine-228305912525.us-central1.run.app`
- **Quality Standard:** Har change enterprise production grade hona chahiye — clean architecture, zero visual ya functional regressions, full accessibility support (TalkBack, WCAG AA contrast), aur passing automated tests.

---

## 2. Project Locations & Workspace Setup

### GitHub Repositories
- **Android UI App (Primary):** `https://github.com/KRFbrothers/teamily-android-ui`
- **Cloud Backend:** `https://github.com/KRFbrothers/dua-backend`
- **Archive / Legacy Reference:** `https://github.com/KRFbrothers/Dua-ai`

### Local Laptop Workspace
- **Folder Structure:** `teamily-android-ui` aur `dua-backend` ko local workspace directory me side-by-side separate folders me rakhein. Dono repos ko kabhi ek folder me merge na karein.
- **Android Studio (Windows):** Open `teamily-android-ui` in **Android** view for Gradle builds, previews, emulator runs, and device deployment. Open `dua-backend` in a separate **New Window** in **Project** view.
- **Android SDK Path (Windows):** `C:\Users\Fareed\AppData\Local\Android\Sdk`
  - Physical testing device: **Realme 14 Pro+ 5G**
  - Emulator testing: **`Pixel_5` AVD**

---

## 3. Strict Engineering & Agent Rules

### 3.1 Padhna Pehle, Likhna Baad Me
- Kisi bhi file ke baare me baat karne se pehle usko **actually padho** (`read_file`, `grep_files`). Memory, conversation history ya training data se file quote mat karo.
- Jo file is run me nahi padhi, uske baare me koi claim mat karo. "Maine padha, isme X hai (line N)" — ye required hai.
- Stale context par rely mat karo; har run me target file ko verify karo.

### 3.2 Assumption Zero — Missing Cheez Maango (4 Options + 1 Recommendation)
- Do **not** guess requirements, invent new folders, or add unrequested architectural layers.
- Version numbers, API signatures, deprecation status me koi guess nahi. Official sources se confirm karo.
- Agar koi requirement, design choice ya next step ambiguous hai: **stop**, simple Hinglish me **4 clear options + 1 recommendation** present karo, aur user approval ka wait karo.
- "Pata nahi / verify nahi hua" bolna allowed aur encouraged hai. Galat number bolne se behtar hai sach batana.

### 3.3 Bina Explicit Permission Code Change Nahi
- User ne "audit karo / check karo / report do" kaha → **sirf report do**. Ek line bhi edit mat karo.
- User ne "fix karo / apply karo" kaha → tab change karo, par **sirf wahi** jo bola gaya.
- Audit ke dauran extra bugs milein → report me mention karo, par bina permission fix mat karo.
- File delete, rename ya move karna — hamesha explicit user permission ke baad.

### 3.4 Work Only Inside Existing Files & Zero Duplication
- Hamesha existing repository structure inspect karo aur files ko in-place update/edit karo.
- Never create parallel ya experimental versions of existing screens, themes, network clients, or viewmodels.
- Zero duplication: No duplicate classes, UI components, color/typography tokens, ya helper utilities. Reuse existing design system and theme tokens.

### 3.5 Scope Creep Prevention — Sirf Jo Bola Gaya
- Jo user ne nahi bola, wo add mat karo: naye unrequested features, naye files, extra dependencies, unused imports.
- Ek deprecation fix ke saath poora codebase refactor/cleanup mat karo unless requested.

### 3.6 Line Numbers — Grep Se, Memory Se Nahi
- Koi bhi line number report karne se pehle `grep -n` ya exact file read se confirm karo.
- Report me line number ke saath exact text snippet bhi do taaki verify kiya ja sake.
- Edit karne ke baad line numbers shift ho jate hain, isliye post-edit re-check karo.

### 3.7 "Jo Verify Nahi Kar Paya" — Alag Section, Hamesha
- Har report ke aakhir me ek mandatory section: **"Jo verify nahi kar paya"** (Table format: Kya | Kyun).
- Real device render, physical touch response, ya environment constraints jahan execute nahi huye, wahan honestly mention karo.
- "Sab verify ho gaya" kabhi mat likho jab tak har single test/build pass na hua ho.

### 3.8 Compile / Build — Truthful Reporting
- Jab local environment me Gradle/Android SDK accessible ho (`./gradlew testDebugUnitTest`, `./gradlew assembleDebug`), command chala kar actual output check karo.
- Agar kisi lightweight agent run ya sandbox me build tools na hon, clearly state: "Compile/build verify nahi hua". Guess mat karo ki "code compile ho jayega".

### 3.9 Report Format — Hinglish, File-Wise Table
- Reports **Hinglish** me — clear, professional, casual tone, aur technical terms English me.
- Har deliverable me **File-wise table** do:
  | File | Line | Pehle | Ab | Kyun |
- Report structure:
  1. **TL;DR** (1-2 lines)
  2. **Kya badla / kya mila** (File-wise table)
  3. **Kya nahi chhua** (Scope proof)
  4. **Verification** (Commands, test runs)
  5. **Jo verify nahi kar paya** (Table)
  6. **Aage kya** (Next steps / PR status)

### 3.10 Deprecated API — Official Sources Se
- Kisi API ko deprecated ya removed bolne se pehle official developer.android.com documentation ya androidx release notes se confirm karo.
- Deprecation level specify karo: `WARNING` ya `ERROR` (build breaker).

### 3.11 Enterprise Android & Compose-Specific Rules
- **Material 3 Tokens:** Colours sirf `MaterialTheme.colorScheme` se. Hardcoded `Color(0xFF...)` nahi (sirf `Color.kt` me).
- **Typography:** Sirf `MaterialTheme.typography` se. Font weights `Type.kt` me defined hain (Lato 400/500/700/900).
- **Accessibility & Touch Targets:** Minimum 48dp touch targets, TalkBack accessibility labels (`contentDescription`), WCAG AA contrast (4.5:1 text, 3:1 UI controls), edge-to-edge system insets.
- **Imports:** Wildcard imports (`import x.*`) strictly prohibited. Explicit imports only.
- **File Naming:** Kotlin conventions follow karein — screens and classes in `PascalCase.kt`.

### 3.12 Security & Secret Hygiene
- **Never** stage, commit, or push API keys, tokens, `.env` files, `google-services.json`, `local.properties`, or signing keystores (`*.jks`, `*.keystore`).

### 3.13 Autonomous Execution & PR Delivery
- Verified work ko local branch me stranded mat chhodo.
- Always create a clean feature or fix branch (`feat/...`, `fix/...`), verify tests/build, push to GitHub, aur GitHub CLI (`gh pr create`) se PR open karo with Hinglish summary.

---

## 4. Required Tools Reference

| Tool | Purpose | Source / Verification |
|---|---|---|
| **Android Studio** | IDE, Layout Inspector, App Inspection | [developer.android.com/studio](https://developer.android.com/studio) |
| **OpenJDK 17 / 21 & Gradle** | Build toolchain | Bundled in Android Studio / [adoptium.net](https://adoptium.net) |
| **Android SDK & `adb`** | API 34/35 platform tools, debugging | SDK Manager / `C:\Users\Fareed\AppData\Local\Android\Sdk` |
| **Git & GitHub CLI (`gh`)** | Version control & PR automation | `git --version`, `gh auth status` |
| **Node.js (v24+) & npm** | `dua-api` gateway & tooling | `node --version`, `npm --version` |
| **Python (3.12+)** | `dua-ai-engine` & test runners | `python --version` |
| **REA (Reverse Engineer Anything)** | UIAutomator live hierarchy, APK analysis | `rea doctor` |
| **Google Cloud CLI (`gcloud`)** | Cloud Run inspection & deployment | `gcloud --version` |

---

## 5. Tool Integration Setup (Multi-Tool Auto-Load)

Taaki har AI tool is rulebook ko automatically recognize aur load kare:

- **Gemini CLI / Antigravity:** `.gemini/settings.json` me `"context": { "fileName": "AGENTS.md" }` configured.
- **Claude Code:** Root me `CLAUDE.md` me `@AGENTS.md` import mapped.
- **Aider:** `.aider.conf.yml` me `read: AGENTS.md` configured.
- **Cursor / Codex / Copilot / Windsurf / Zed:** Natively repo root me `AGENTS.md` auto-read karte hain.

---

## 6. Pre-Delivery Checklist

Har deliverable ya PR se pehle ye verify karo:

- [ ] Target files actually padhi hain (memory se nahi)
- [ ] Har line number grep/read se confirmed
- [ ] Sirf requested scope me kaam kiya gaya (no scope creep)
- [ ] Material 3 tokens, Lato font, 48dp touch targets verified
- [ ] Unit tests / build verified via Gradle
- [ ] Report me file-wise table present hai
- [ ] Report ke aakhir me "Jo verify nahi kar paya" table included hai
- [ ] Secrets/keys leak nahi huye (`.gitignore` respected)
- [ ] Clean Git branch banayi aur PR open ki

---

## 7. Ek Line Me

> **Padho phir likho. Assume mat karo. Permission lo. Existing files reuse karo. Jo verify nahi hua, saaf batao.**
