# AGENTS.md

Rules for any agent or human in `teamily-android-ui`. Read before touching a file. A rulebook, not a history.

**Citations.** Name the file plus a function, class, constant or heading, never a line number. Numbers rot in one or two commits: the previous revision of this file carried 94 of them, and the backend repo watched `API_CONTRACT.md` shift 15 to 25 lines under one new section at the top. A name survives that. Label each code claim `FACT` (read in the file), `ASSUMPTION` (your guess) or `Not found in code`. Never present an assumption as a fact. `CLAUDE.md`, `.aider.conf.yml` and `.gemini/settings.json` all point at this file, so every agent reads what you write here.

## 1. Shape

Kotlin plus Jetpack Compose, one Gradle module `:app`, `rootProject.name = "DuaGenAI"` (`settings.gradle.kts`). The Gradle root is the repo root; there is no `android/` directory. `applicationId` and `namespace` are `ai.duagenai.design`. 42 Kotlin files sit under `app/src/main/java`, 33 in `screens/`, 6 in `theme/`. The app is a UI client for two backends, the Node API and the Python engine; both hosts arrive as generated `BuildConfig` fields resolved by `ApiConfig`, never as a literal in a screen. `README.md` is one line. `documents/M3-design_v01/` is a tracked design archive, not build input.

**Not found in code:** no backend, no `CONTRIBUTING.md`, no `gradle/libs.versions.toml`, no Room, no `app/schemas/`, no KSP or KAPT, no `MigrationTest`, no `supabase/`, no `webpages/`, no Dependabot config. `FACT:` a `pull_request` CI trigger does exist, in `.github/workflows/pr-checks.yml` under `on: pull_request: branches: [main]`; it runs `:app:testDebugUnitTest` only, not `assembleDebug` and not `lintDebug`.

## 2. Toolchain

AGP `9.2.0` and Kotlin Compose plugin `2.2.21` (`build.gradle.kts` root), Gradle wrapper `9.6.0` (`gradle-wrapper.properties`), CI JDK 21 Temurin. `compileSdk` 37 with `minorApiLevel` 1, `targetSdk` 36, `minSdk` 24, `versionCode` 1, `versionName` `"1.0"` (`defaultConfig`). `sourceCompatibility` and `targetCompatibility` 17, `jvmTarget` `JVM_17`.

`FACT:` the Kotlin Android plugin is not applied, only `com.android.application` and `org.jetbrains.kotlin.plugin.compose`, so Kotlin comes from AGP 9 built-in support. `FACT:` `compileSdk 37.1` is unsupported and its warning is suppressed by `android.suppressUnsupportedCompileSdk` in `gradle.properties`. `FACT:` `core-ktx` 1.13.1 and `activity-compose` 1.9.0 are 2024 libraries in a 2026 stack; Compose BOM `2026.11.01` ships Material3 1.5.0. `isMinifyEnabled` is false and no `signingConfig` is declared, so a release build is unsigned and unminified. `gradle.properties` sets `-Xmx4096m` and turns the configuration cache, parallel build and build cache on, so any new task must be configuration-cache safe.

## 3. Commands

Run from the repo root.

```bash
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:testDebugUnitTest          # JVM unit tests, the only gate CI runs
./gradlew :app:lintDebug                  # Android lint, CI does not run it
./gradlew :app:connectedDebugAndroidTest  # needs a device, CI does not run it
```

Point the app elsewhere without editing source: `-PduaApiBaseUrl`, `-PduaApiBaseUrlDebug` or `DUA_API_BASE_URL`, same pattern for the engine, per `apiBaseUrlFor` in `app/build.gradle.kts`.

`FACT:` CI is `debug-apk-release.yml`, name `Debug APK Release`. It runs `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace`, then publishes a release tagged `debug-apk-<run_number>` and refuses to overwrite an existing tag. It never runs `lintDebug`, `assembleRelease` or `connectedDebugAndroidTest`, and has no secret scan and no audit. `FACT:` its SDK step tries `platforms;android-37.1` then `platforms;android-37` and ends with `|| true`, so an SDK install failure is swallowed and surfaces later as a less clear error.

**A build may be impossible where you run.** Two independent blockers were hit here: no Android SDK (`ANDROID_HOME` unset, `sdkmanager` absent, fetching the SDK tools not permitted), and `-Xmx4096m` against about 773 MB of RAM makes the Gradle daemon disappear. Never claim a build or a unit test passed unless you ran it and read the result.

## 4. What must never be committed

`.gitignore` blocks `.gradle/`, `build/`, `local.properties`, `*.iml`, `.idea/`, `.kotlin/`, `*.apk`, `*.aab`, `*.ap_`, `*.dex`, `*.class`, `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties`, `google-services.json`, `.env`, `.env.*`, `*.log` and `node_modules/`.

`FACT:` no keystore, `.env` or signing file appears anywhere in git history. Check with `git log --all --pretty=format: --name-only --diff-filter=A | sort -u | grep -icE 'keystore|\.jks|\.keystore|\.env$|\.env\.|signing|\.p12|\.pem'` and expect 0; use `--all`, not `HEAD`, or the number tells you nothing about the history. `FACT:` the CI debug APK is signed by the throwaway debug keystore AGP generates on the runner, so no signing secret is needed and none is stored.

Before any push read `git status --short` and `git diff --cached` line by line. A committed secret is reported and rotated, never quietly deleted in a later commit.

## 5. Branch and PR

`FACT:` observed branches are `feat/<topic>` and `fix/<topic>` in kebab case, merged through PRs (`fix/chat-markdown-timeout-and-polish`, `fix/audit-p0-p1-theme-a11y-state`, `docs/agents-md-fix-false-claims` in `git log`).

## 6. Rules

**6.1 Read-only by default.** Change nothing until the task asks, then the smallest safe change.

**6.2 Ambiguous task: ask, do not guess.** 4 concrete options plus one marked `(Recommended)`, and one line on what the wrong pick costs. Never build the wrong thing and report it as done.

**6.3 One phase, then stop.** Report it, stop for review, state what you did not do. If a command fails, retry with a fix at most 3 times, then stop with the exact command, the error and what changed.

**6.4 Never push to `main`.** Push a branch, open a PR, and get an explicit owner yes in the PR thread before merging or publishing a release, every time. `FACT:` two workflows exist. `.github/workflows/pr-checks.yml` runs on `pull_request` to `main` and runs `:app:testDebugUnitTest` with `contents: read`. `.github/workflows/debug-apk-release.yml` runs on `push` to `main` and on `workflow_dispatch`, and it publishes a GitHub Release and a `debug-apk-<run_number>` tag. So a PR is tested before merge, but the APK build and the release still happen only after a push to `main`, which is why a direct push publishes a release from unreviewed code.

**6.5 Every PR carries a short Hinglish summary.** 3 to 6 lines: what changed, why, what you ran, what you could not run, and the risk. The owner decides from that, not from the diff.

**6.6 Never weaken a test to make it pass,** and never claim a check passed that you did not run. No deleted assertion, no `@Ignore`, no loosened expected value, no changed `@Config`. Fix the code, or report the failure with the error. If a test is genuinely wrong, say why in the PR body and change it in its own commit. `FACT:` the JVM unit tests are a release gate ahead of `assembleDebug` in CI. `FACT:` no linter or formatter is configured beyond `kotlin.code.style=official`, so never write "lint passed" without running `lintDebug`.

**6.7 Every claim carries a file path** plus a function, class, constant or heading. Re-read the file at the commit you report on. No bare assertion, no inferred behaviour, no invented test result, no number carried over from the backend repo.

**6.8 Nothing validates this file but a human.** `FACT:` no CI job reads it, unlike the backend repo where a unit test fails the PR on drift. A wrong edit here fails nothing and misleads every agent that reads it.

**6.9 In-place edits only, no duplicates.** Update the canonical file, never `*V2.kt`, `*New.kt`, `*Copy.kt` or an unrequested helper. Search for an existing equivalent first.

**6.10 No dependency add or upgrade without owner approval.** Versions live directly in `app/build.gradle.kts`; there is no version catalog.

**6.11 Compose only, no XML UI.** `themes.xml` exists only for the launch window theme. Do not add layouts, and do not port a web UI.

**6.12 Zero hardcoded colors in screens.** No `Color(0xFF...)`, `Color.White`, `Color.Black`, `Color.Gray` under `screens/`. Use `MaterialTheme.colorScheme.*` or the tokens in `theme/` (`Color.kt`, `TeamilyTokens.kt`, `DuaGenAITheme.kt`). `FACT:` the rule holds today, 0 matches.

**6.13 48dp touch targets.** Every interactive element keeps at least 48dp by 48dp. `TouchTargets.kt` exists to enforce it.

**6.14 Strings in `strings.xml`.** `app/src/main/res/values/strings.xml` is the one copy source. `FACT:` the rule is not followed everywhere: 74 `text = "..."` literals remain in `screens/`. Do not add more, and do not mass-rewrite them inside an unrelated PR.

**6.15 UI design freeze.** On a data or API task, do not change colors, typography, spacing, shapes or navigation unless the task asks.

**6.16 No database here, so no migration rules apply.** `FACT:` no Room, no schema directory, no migration file, no SQL. The staging-only, never-edit-a-migration and mandatory-rollback rules live in the backend repo's `AGENTS.md`. If a future change adds a local database, add those rules to this file in the same PR, and ship a migration plus a rollback, never a schema by hand.

**6.17 One official agent, `Dua Work`; `personal-agent` is removed.** Never re-add it, never call it official, never add a second pinned agent without an owner decision recorded in the backend repo, and do not invent a skill list. `FACT:` the code lags. `DuaAgentSkills` in `ComposerSheets.kt` still hardcodes five starter skills. `BaseRemoteRepository.kt` still maps `personal-agent` to "Personal Agent" and still blocks delete for both `personal-agent` and `dua-agent`. `InMemorySkillsDataSource` seeds every skill with `isStarter = true` and refuses to delete a starter. `HomeScreen.kt` keeps `PERSONAL_AGENT_TITLE` as the pinned hero card. 66 matches for `personal-agent` across `app/src`. Work to do, not truth to copy.

## 7. Cross-repo

`API_CONTRACT.md` lives in `KRFbrothers/dua-backend` and the app never names it. What the app does cite is the backend's SSE wire format, in comments in `ChatTranscriptStore.kt` and `ChatTranscriptStoreTest.kt` pointing at `sse.mjs`. Treat `API_CONTRACT.md` as the contract of record by convention and update it in the same coordinated change as any shape you touch.

Do not change these without a matching backend change: the `X-Dua-Native: 1` header sent on every call including anonymous ones, the no-redirect policy on authenticated requests (`instanceFollowRedirects` false, there to keep the bearer token from following a 3xx to another host), the auth header handling, the SSE `delta` field name, or `POST /api/chat` on either process. Personal chat goes to the Node API, agent chat to the Python engine; both are cross-repo surfaces.

## 8. Done

Behaviour implemented. Existing file edited in place, nothing duplicated, no unrelated file touched. `./gradlew :app:testDebugUnitTest` run and the result reported honestly, or the reason it could not run stated with the exact error. `lintDebug` run or its absence stated. No new hardcoded color, hardcoded string or sub-48dp target. Diff read for secrets. Every limitation named with the check you could not run.

## 9. Known defects, do not repeat them

A hardcoded shared secret `sheetSecretKey` and a hardcoded Apps Script URL with a key, both in `ApiConfig`; `SheetBackendClient` still ships that secret. `allowBackup` enabled in `AndroidManifest.xml`. The session token stored in `noBackupFilesDir`, app-private but not encrypted and with no client expiry, per the comment in `SessionStore`. CI swallowing the SDK install failure. Release unsigned, minify off. `versionCode` never incremented. Demo conversation data with a real-looking handle reachable in `DuaAgentScreens`. One 5265-line test class in `nav/`. Robolectric pinned to `@Config(sdk = [34])` in all four test files while `compileSdk` is 37.1. `RECORD_AUDIO` declared but never requested from Kotlin.

## 10. Why this file replaced the last two

The first revision made eight false claims: Room, ViewModel, `app/schemas/`, `gradle/libs.versions.toml`, KSP and `MigrationTest.kt` as existing things, plus a non-existent `android/` root. The second fixed those but carried 94 exact line-number citations that break on the next edit, and repeated facts that belong only to the backend repo. Every agent reading either looked for files that do not exist. This revision keeps the verified facts and drops the fragile part.

## 11. Open items

`personal-agent` and the five-skill starter set are still in the UI against 6.17. No Kotlin linter or formatter. `core-ktx` and `activity-compose` two years behind the rest of the stack. No release signing. `documents/M3-design_v01/` duplicates Kotlin sources already under `app/src/main/java` and will drift.
