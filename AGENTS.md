# AGENTS.md — Dua Gen AI Engineering & Workflow Guide

## 1. Project Overview
**Dua Gen AI** is a production-grade, enterprise-level Android AI assistant application built with Kotlin, Jetpack Compose, and Material 3, backed by a cloud-native AI and API platform.

- **Android Client (`teamily-android-ui`):** Native Android application featuring a Material 3 Forest-Slate design system, Lato typography, a 5-tab bottom navigation dock (`Chats`, `Agents`, `Discover`, `Memory`, `Artifacts`), two pinned agents (`Personal Agent` in fast `auto` mode and `Dua Agent` in `agent` mode with 5 specialized skills), and full navigation through `DuaGenAINavHost`.
- **Cloud Backend (`dua-backend`):** Separated backend repository running a Node.js API gateway (`dua-api` on Google Cloud Run), a Python Flask/ADK AI execution engine (`dua-ai-engine` on Google Cloud Run), and a Supabase PostgreSQL database with vector memory support.
- **Quality Standard:** Every change must meet the engineering bar of a large-scale production Android company—clean architecture, zero visual or functional regressions, full accessibility support, and passing automated tests.

---

## 2. Project Locations (GitHub & Laptop)

### GitHub Repositories
- **Android UI App (Primary):** `https://github.com/KRFbrothers/teamily-android-ui`
- **Cloud Backend:** `https://github.com/KRFbrothers/dua-backend`
- **Archive / Legacy Reference:** `https://github.com/KRFbrothers/Dua-ai`

### Local Laptop Workspace
- **Folder Structure:** Keep `teamily-android-ui` and `dua-backend` in separate, side-by-side folders inside the user's local workspace directory (both on Windows and Linux environments). Never merge both repositories into a single folder.
- **Android Studio Windows:** Open `teamily-android-ui` in **Android** view for Gradle builds, previews, emulator runs, and device deployment. Open `dua-backend` in a separate **New Window** in **Project** view for server edits and deployments.
- **Android SDK Path (Windows):** `C:\Users\Fareed\AppData\Local\Android\Sdk` (with physical device testing on Realme 14 Pro+ 5G and emulator testing on `Pixel_5` AVD).

---

## 3. Strict Engineering Rules

1. **Work Only Inside Existing Project Files**
   - Always inspect the existing repository structure first and edit, update, or replace existing files in place.
   - Never create parallel or experimental versions of existing screens, themes, network clients, or view models.

2. **Zero Duplication Policy**
   - Do **not** create duplicate files, duplicate classes, duplicate UI components, duplicate color/typography tokens, or duplicate helper utilities.
   - Always reuse the existing design system, shared modifiers, theme tokens, and network layer.

3. **Zero Unverified Assumptions**
   - Do **not** guess requirements, invent new folders, or add unrequested architectural layers on your own.
   - If a requirement, design choice, or next step is ambiguous, stop and present **4 clear options + 1 recommendation** in simple Hinglish, and wait for user approval before editing files.

4. **Enterprise-Grade Android Standards**
   - Maintain strict Material 3 compliance, WCAG AA contrast (4.5:1 for text, 3:1 for UI controls), minimum 48dp touch targets, TalkBack accessibility labels, and proper edge-to-edge system bar insets.
   - Respect the UI Design Freeze: never alter approved colors, typography, spacing, or the 5-tab navigation unless explicitly requested.
   - Always run the full unit test suite and debug build verification after making changes.

5. **Security & Secret Hygiene**
   - Never stage, commit, or push API keys, tokens, `.env` files, `google-services.json`, `local.properties`, or signing keystores (`*.jks`, `*.keystore`).

6. **Autonomous Execution & PR Delivery**
   - Handle all file edits, terminal verification, and error fixes autonomously without asking the user to manually edit files.
   - Never leave verified work stranded locally: always create a clean feature or fix branch, push to GitHub, and open a Pull Request with a concise Hinglish summary (starting with a 1–2 line TL;DR).

---

## 4. Required Tools & Missing Tool Installation Guide

Before starting any task, verify that the required tools are installed and accessible on the machine. **If any tool is missing, do not guess or skip the step—guide the user with the exact download source and installation steps below.**

### Core Android & Version Control Tools
- **Android Studio** (IDE, Gradle runner, Compose Split Preview, Layout Inspector, App Inspection, Device Manager)
  - *If missing:* Download the official installer from [developer.android.com/studio](https://developer.android.com/studio), run the setup wizard, and accept the default Android SDK installation.
- **JetBrains OpenJDK 17 / 21 & Gradle Wrapper** (Build toolchain)
  - *If missing:* Use Android Studio's bundled JDK under Settings -> Build, Execution, Deployment -> Build Tools -> Gradle, or download OpenJDK 21 from [adoptium.net](https://adoptium.net/).
- **Android SDK Platforms (API 34 / 35), Build-Tools & Platform-Tools (`adb`)**
  - *If missing:* Open Android Studio -> Settings -> Languages & Frameworks -> Android SDK, install Android 14/15 SDKs and SDK Platform-Tools. If `adb` is not recognized in the terminal, add the SDK `platform-tools` folder to the system `PATH` environment variable and restart Android Studio.
- **Git** (Version control)
  - *If missing:* Download from [git-scm.com](https://git-scm.com/) and install with default options.
- **GitHub CLI (`gh`)** (Creating branches and Pull Requests from terminal)
  - *If missing:* Download from [cli.github.com](https://cli.github.com/) and authenticate with the `KRFbrothers` GitHub account.

### Backend, Inspection & Automation Tools
- **Node.js (v24+) & npm** (Required for `dua-api` and MCP tools)
  - *If missing:* Download the LTS/Current installer from [nodejs.org](https://nodejs.org/).
- **Python (3.12+)** (Required for `dua-ai-engine` and verification scripts)
  - *If missing:* Download from [python.org/downloads](https://www.python.org/downloads/) and ensure "Add Python to PATH" is checked during installation.
- **REA (Reverse Engineer Anything) & Android Inspection Tools** (Live device UI hierarchy via UIAutomator, APK analysis via Apktool, binary/network inspection)
  - *If missing:* Install REA globally via npm (`rea-agents` from [github.com/morluto/rea](https://github.com/morluto/rea)), verify setup with `rea doctor`, and download Apktool from [apktool.org](https://apktool.org/) if APK resource inspection is needed.
- **Google Cloud CLI (`gcloud`)** (Optional, for checking or deploying `dua-backend` on Cloud Run)
  - *If missing:* Download from [cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install) and sign in to the project's Google Cloud account.
