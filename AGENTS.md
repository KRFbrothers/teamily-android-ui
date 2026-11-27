# AGENTS.md

Scope: the `teamily-android-ui` repository only. Read this before touching any
file. Every claim carries a `file:line` citation. Unconfirmed claims are marked
`ASSUMPTION`. Absent things are marked `Not found in code`.

Verified against commit `ec77370290ec509b61a209add87cb36ebd599f2e`
(`main`, 2026-11-27).

---

## 1. Repo purpose and scope

**FACT:** This repository is the Android app for Dua Gen AI, built with Kotlin
and Jetpack Compose. Source: `settings.gradle.kts:23`
(`rootProject.name = "DuaGenAI"`), `app/build.gradle.kts:66`
(`namespace = "ai.duagenai.design"`).

**FACT:** The app is a UI client. It talks to two backend processes: the Node
API and the Python engine. Source:
`app/src/main/java/ai/duagenai/design/screens/ApiConfig.kt:85-132`
(`BASE_URL` and `ENGINE_BASE_URL`), and
`app/src/main/java/ai/duagenai/design/screens/ChatTranscriptStore.kt:197-198`.

**FACT:** The README is one line and describes only three screens: "The
Sidebar, Home screen (Dua home) and Agent screen." Source: `README.md:1`.

**FACT:** The app has 42 Kotlin source files under `app/src/main/java`.
Source: `find app/src/main/java -name "*.kt" | wc -l`.

**What is NOT in this repo:**

- **FACT:** No backend code. The backend is `KRFbrothers/dua-backend`.
- **FACT:** No `CONTRIBUTING.md`. Not found in code.
- **FACT:** No `gradle/libs.versions.toml`. Not found in code. Versions are
  written directly in the build files.
- **FACT:** No Room database. `grep -rn "androidx.room\|RoomDatabase\|@Entity\|@Dao"`
  returns 0 matches. Not found in code.
- **FACT:** No `app/schemas/` directory. Not found in code.
- **FACT:** No KSP and no KAPT. `grep -rn "ksp\|kapt" --include=*.kts` returns
  0 matches. Not found in code.
- **FACT:** No `MigrationTest.kt`. Not found in code.
- **FACT:** No `android/` directory. The Gradle project root is the repository
  root. Source: `ls -d android` fails, `settings.gradle.kts:24` (`include(":app")`).

> **Important:** the previous `AGENTS.md` in this repo claimed Room, ViewModel,
> `app/schemas/`, `gradle/libs.versions.toml`, KSP and `MigrationTest.kt` all
> exist. Five of those six are false. See section 10.

---

## 2. Tech stack and exact versions

| Item | Value | Source |
|---|---|---|
| AGP | `9.2.0` | `build.gradle.kts:6-7` |
| Kotlin Compose plugin | `2.2.21` | `build.gradle.kts:8` |
| Gradle wrapper | `9.6.0` | `gradle/wrapper/gradle-wrapper.properties:3` |
| JDK (CI) | `21`, Temurin | `.github/workflows/debug-apk-release.yml:46-50` |
| `compileSdk` | `37` with `minorApiLevel = 1` | `app/build.gradle.kts:67-71` |
| `targetSdk` | `36` | `app/build.gradle.kts:76` |
| `minSdk` | `24` | `app/build.gradle.kts:75` |
| `applicationId` | `ai.duagenai.design` | `app/build.gradle.kts:74` |
| `namespace` | `ai.duagenai.design` | `app/build.gradle.kts:66` |
| `versionCode` | `1` | `app/build.gradle.kts:77` |
| `versionName` | `"1.0"` | `app/build.gradle.kts:78` |
| `sourceCompatibility` | `VERSION_17` | `app/build.gradle.kts:104` |
| `targetCompatibility` | `VERSION_17` | `app/build.gradle.kts:105` |
| `jvmTarget` | `JVM_17` | `app/build.gradle.kts:131` |

**FACT:** The Kotlin Android plugin (`org.jetbrains.kotlin.android`) is NOT
applied. Only `com.android.application` and
`org.jetbrains.kotlin.plugin.compose` are. Source:
`app/build.gradle.kts:1-4`, `build.gradle.kts:5-9`. This is AGP 9 built-in
Kotlin.

**FACT:** `compileSdk 37.1` is suppressed as unsupported. Source:
`gradle.properties:22` (`android.suppressUnsupportedCompileSdk=37.1`).

### Dependencies

| Dependency | Version | Source |
|---|---|---|
| `androidx.core:core-ktx` | `1.13.1` | `app/build.gradle.kts:136` |
| `androidx.activity:activity-compose` | `1.9.0` | `app/build.gradle.kts:137` |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.8.2` | `app/build.gradle.kts:138` |
| Compose BOM | `2026.11.01` | `app/build.gradle.kts:142` |
| `androidx.graphics:graphics-shapes` | `1.0.1` | `app/build.gradle.kts:153` |
| `androidx.navigation:navigation-compose` | `2.10.2` | `app/build.gradle.kts:155` |
| `androidx.datastore:datastore-preferences` | `1.1.1` | `app/build.gradle.kts:156` |
| `kotlinx-coroutines-android` | `1.8.1` | `app/build.gradle.kts:157` |
| `junit:junit` | `4.13.2` | `app/build.gradle.kts:161` |
| `org.robolectric:robolectric` | `4.12.2` | `app/build.gradle.kts:162` |
| `androidx.test.ext:junit` | `1.1.5` | `app/build.gradle.kts:163` |
| `androidx.test:core` | `1.5.0` | `app/build.gradle.kts:164` |
| `androidx.navigation:navigation-testing` | `2.10.2` | `app/build.gradle.kts:166` |

**FACT:** Compose BOM `2026.11.01` ships Material3 `1.5.0`. Source:
`app/build.gradle.kts:141` (comment: "2026.11.01 ships Material3 1.5.0").

**FACT:** `core-ktx 1.13.1` and `activity-compose 1.9.0` are from 2024 while
the rest of the stack is 2026. Source: `app/build.gradle.kts:136-137`.

**FACT:** No dependency update bot is configured. Not found in code: no
`.github/dependabot.yml` in this repo.

### Build features

| Feature | Value | Source |
|---|---|---|
| `compose` | `true` | `app/build.gradle.kts:109` |
| `buildConfig` | `true` | `app/build.gradle.kts:113` |
| `isMinifyEnabled` (release) | `false` | `app/build.gradle.kts:93` |
| ProGuard file | `proguard-android-optimize.txt` | `app/build.gradle.kts:95` |

**FACT:** Release builds are not minified. Source: `app/build.gradle.kts:93`.

**FACT:** No `signingConfig` is declared for release. `grep -n "signingConfig"
app/build.gradle.kts` returns nothing. Not found in code.

---

## 3. Build and test commands

Run from the repository root (there is no `android/` subdirectory).

```bash
./gradlew assembleDebug              # debug APK
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew lintDebug                  # Android lint
./gradlew connectedDebugAndroidTest  # instrumented tests, needs a device
```

Sources: `AGENTS.md:9-12` (previous file), and CI runs
`./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --stacktrace`
at `.github/workflows/debug-apk-release.yml:73`.

**FACT:** CI runs unit tests and `assembleDebug` only. It does not run
`lintDebug`, `assembleRelease` or `connectedDebugAndroidTest`. Source:
`.github/workflows/debug-apk-release.yml:73` (the only gradle invocation).

**FACT:** CI publishes a debug APK as a GitHub Release asset, one release per
push, tag `debug-apk-<run_number>`. Source:
`.github/workflows/debug-apk-release.yml:102-134`.

**FACT:** CI refuses to overwrite an existing tag. Source:
`.github/workflows/debug-apk-release.yml:108-111`.

**FACT:** CI has a known defect: it tries `platforms;android-37.1` then falls
back to `platforms;android-37`, and ends with `|| true`, so a failure to
install the SDK platform is swallowed. Source:
`.github/workflows/debug-apk-release.yml:64-65`.

**FACT:** `platforms;android-37` (bare) is not a valid package name; the
published name is `platforms;android-37.0`. `ASSUMPTION`: this is why the
fallback exists and why `|| true` was added.

**FACT:** This sandbox cannot download the Android SDK, so no build can be run
here. Do not claim a build passed.

---

## 4. What must never be committed

**FACT:** `.gitignore` blocks these. Source: `.gitignore:12-64`.

| Pattern | Why |
|---|---|
| `.gradle/`, `build/` | Gradle state and output |
| `local.properties` | machine-specific SDK path |
| `*.iml`, `.idea/`, `.kotlin/` | IDE and Kotlin state |
| `*.apk`, `*.aab`, `*.ap_`, `*.dex`, `*.class` | build outputs |
| `*.jks`, `*.keystore`, `keystore.properties`, `signing.properties` | signing material |
| `google-services.json` | Firebase config |
| `.env`, `.env.*` | secrets |
| `*.log` | logs |
| `node_modules/` | web tooling only |

**FACT:** `local.properties` is gitignored. Source: `.gitignore:15`.

**FACT:** No keystore, `.env` or signing file is tracked in git history.
Source: `git log --all --pretty=format: --name-only --diff-filter=A | sort -u |
grep -iE "keystore|\.jks|\.keystore|\.env$|\.env\.|signing|\.p12|\.pem"`
returns 0 matches across all 301 commits.

**FACT:** The debug APK is signed with the throwaway debug keystore AGP
generates on the runner. No signing secret is needed. Source:
`.github/workflows/debug-apk-release.yml:12-14`.

**Rule:** Before any commit, run `git status` and `git diff --cached` and
confirm no keystore, no `.env`, no token, no `local.properties` is staged.

---

## 5. Branch naming and PR rules

**FACT:** Observed branch names follow `feat/...` and `fix/...` with a
kebab-case topic. Source: `git log` shows
`fix/chat-markdown-timeout-and-polish` (PR #115) and
`fix/audit-p0-p1-theme-a11y-state` (PR #116).

**FACT:** The previous `AGENTS.md` requires a clean `feat/...` or `fix/...`
branch and a PR with a Hinglish summary, and requires approval before merging
or deleting branches. Source: `AGENTS.md:36`.

**FACT:** CI runs on `push` to `main` and on `workflow_dispatch`. It does NOT
run on `pull_request`. Source: `.github/workflows/debug-apk-release.yml:18-21`.

**FACT:** CI needs `contents: write` to publish the release. Source:
`.github/workflows/debug-apk-release.yml:25-26`.

**FACT:** CI does not cancel an in-progress run, to avoid a half-created
release. Source: `.github/workflows/debug-apk-release.yml:30-32`.

**Rule (derived):** branch as `feat/<topic>` or `fix/<topic>`, open a PR, and
note that this repo has no PR-triggered CI, so a green run on `main` is the
only automated signal.

---

## 6. Rules for an agent working in this repo

### 6.1 Read-only by default

Start read-only. Do not write, move, rename or delete anything until the task
explicitly asks for a change.

### 6.2 Every claim needs a file and line

State the path and line for every factual claim. If a thing is not in the
code, write `Not found in code`. Do not infer a file's contents from its name.

### 6.3 Fact and Assumption are separate lines

Label each statement `FACT:` or `ASSUMPTION:`. Never present an assumption as
a fact.

### 6.4 One phase, then stop

Do one phase of work, report it, and stop for review.

### 6.5 Secret scan before any push

Run `git status` and `git diff` and confirm no secret is staged. See section 4.

### 6.6 Maximum 3 self-heal attempts

If a command fails, retry with a fix at most 3 times. After the third failure,
stop and report the exact error, the command, and what you tried.

### 6.7 In-place edits only, no duplicates

**FACT:** Search existing files first and update canonical files in place.
Never create `*V2.kt`, `*New.kt`, `*Copy.kt` or unrequested helper files.
Source: `AGENTS.md:29`.

### 6.8 Do not add or upgrade dependencies without approval

**FACT:** Do not add or upgrade libraries without explicit user approval.
Source: `AGENTS.md:31`.

### 6.9 No XML UI layouts

**FACT:** UI is 100% Jetpack Compose. Do not create XML layouts. `themes.xml`
is only for the launch window theme. Source: `AGENTS.md:4`.

**FACT:** `app/src/main/res/values/themes.xml` exists and is 618 bytes.
Source: `ls -la app/src/main/res/values/`.

### 6.10 Zero hardcoded colors

**FACT:** Never write `Color(0xFF...)`, `Color.White`, `Color.Black` or
`Color.Gray` in screen composables. Use `MaterialTheme.colorScheme.*` or the
theme tokens in `theme/`. Source: `AGENTS.md:14`.

**FACT:** This rule is followed. `grep -rn "Color(0xFF\|Color\.White\|Color\.Black\|Color\.Gray"`
over `app/src/main/java/ai/duagenai/design/screens/` returns 0 matches.

**FACT:** The theme tokens live in `app/src/main/java/ai/duagenai/design/theme/`
(`Color.kt`, `DuaGenAITheme.kt`, `Shape.kt`, `TeamilyEffects.kt`,
`TeamilyTokens.kt`, `TeamilyTypography.kt`). Source: `ls -la` of that
directory. Note the path is `design/theme/`, not `design/ui/theme/`.

### 6.11 48dp touch targets

**FACT:** Every interactive element keeps at least 48dp by 48dp. Source:
`AGENTS.md:16`.

**FACT:** `app/src/main/java/ai/duagenai/design/screens/TouchTargets.kt`
exists to enforce this. Source: file listing.

### 6.12 Strings in `strings.xml`

**FACT:** All user-facing text goes in `app/src/main/res/values/strings.xml`.
Source: `AGENTS.md:25`.

**FACT:** `strings.xml` exists and is 11048 bytes. Source:
`ls -la app/src/main/res/values/`.

**FACT:** The rule is not fully followed. `grep -rn 'text = "[A-Za-z]'` over
`app/src/main/java/ai/duagenai/design/screens/` returns 74 matches, the
largest concentrations being `ArtifactsScreen.kt` (17) and `LiveFlows.kt`
(15). Source: that grep.

### 6.13 No skipped tests

**FACT:** Never mark a failing test `@Ignore` or delete assertions to force a
green build. Source: `AGENTS.md:35`.

### 6.14 UI design freeze

**FACT:** When working on data or API tasks, do not change colors, typography,
spacing, shapes or navigation unless explicitly requested. Source:
`AGENTS.md:27`.

### 6.15 Do not run a build you cannot run

**FACT:** This sandbox cannot download the Android SDK. Do not attempt
`./gradlew` and do not claim a build or test passed.

---

## 7. Cross-repo rules

**FACT:** `API_CONTRACT.md` lives in the `dua-backend` repo, not in this one.
`find . -name API_CONTRACT.md` returns nothing here, and
`grep -rn "API_CONTRACT" app/src` returns 0 matches. The app does not cite it.

**FACT:** What the app actually cites is the backend's SSE wire format. Source:
`app/src/main/java/ai/duagenai/design/screens/ChatTranscriptStore.kt:317-319`
("see the backend's `apps/api/src/core/sse.mjs` wire-format comment").
`grep -rn "sse.mjs" app/src` returns 2 matches, both comments
(`ChatTranscriptStore.kt:318`, `ChatTranscriptStoreTest.kt:18`).

**ASSUMPTION:** `API_CONTRACT.md` is still the intended contract of record for
request and response shapes. The app never names it, so this is a convention,
not something the code proves.

**Rule:** Any endpoint change must update `API_CONTRACT.md` in the backend repo
in the same coordinated change. Never change a shape on one side only.

**FACT:** The client sends `X-Dua-Native: 1` on every call, including
anonymous ones, and never sends `Origin`. Source:
`app/src/main/java/ai/duagenai/design/screens/JsonHttpClient.kt:283` and
`:333`.

**FACT:** The client does not follow redirects on authenticated requests, to
avoid leaking the bearer token. Source:
`app/src/main/java/ai/duagenai/design/screens/JsonHttpClient.kt:287-294` and
`:335`.

**Rule:** Do not change the `X-Dua-Native` header, the no-redirect policy, or
the auth header handling without a coordinated backend change.

**FACT:** The app calls the Node API for personal chat and the Python engine
for agent chat. Source:
`app/src/main/java/ai/duagenai/design/screens/ChatTranscriptStore.kt:197-198`
(`if (isEngine) ApiConfig.engineUrl("/api/chat") else ApiConfig.url("/api/chat")`).

**Rule:** A change to `POST /api/chat` is a cross-repo change. Check both
processes.

---

## 8. Definition of done

A change is complete only when:

- The requested behavior is implemented.
- Existing files were edited in place, not duplicated.
- No unrelated file changed.
- `./gradlew testDebugUnitTest` was run and its result reported honestly.
- `./gradlew lintDebug` was run, or the reason it could not be run is stated.
- No hardcoded color, no hardcoded string, no sub-48dp touch target was added.
- The diff was reviewed for secrets.
- Any remaining limitation is reported.

---

## 9. Known defects in this repo (do not repeat them)

These are recorded so an agent does not copy the pattern.

| Defect | Location |
|---|---|
| Hardcoded shared secret in source | `app/src/main/java/ai/duagenai/design/screens/ApiConfig.kt:156` (`sheetSecretKey = "DUA_2026_PIN"`) |
| Hardcoded Apps Script URL with key | `app/src/main/java/ai/duagenai/design/screens/ApiConfig.kt:152` |
| Dead client that ships the secret | `app/src/main/java/ai/duagenai/design/screens/SheetBackendClient.kt` |
| `allowBackup` enabled | `app/src/main/AndroidManifest.xml:14` |
| Session token stored unencrypted | `app/src/main/java/ai/duagenai/design/screens/SessionStore.kt:62` (comment: "app-private but NOT encrypted") |
| CI swallows SDK install failure | `.github/workflows/debug-apk-release.yml:64-65` |
| `compileSdk 37.1` unsupported, warning suppressed | `gradle.properties:22` |
| Release build unsigned | `app/build.gradle.kts:92-100` (no `signingConfig`) |
| `versionCode` never incremented | `app/build.gradle.kts:77` |
| Personal demo data in a reachable screen | `DuaAgentScreens.kt:1332,1340,1348` |
| 5265-line single test class | `app/src/test/java/ai/duagenai/design/nav/DuaGenAINavHostTest.kt` |
| Robolectric pinned to SDK 34 while `compileSdk` is 37.1 | `app/src/test/java/ai/duagenai/design/nav/DuaGenAINavHostTest.kt:699` (`@Config(sdk = [34])`); same pin at `ChatBarTest.kt:30`, `ChatTranscriptStoreTest.kt:26`, `CreateDocumentWiringTest.kt:23`. `app/build.gradle.kts:162` only declares the dependency, it does not pin the SDK |
| `RECORD_AUDIO` declared but never requested | `app/src/main/AndroidManifest.xml:11`; `grep -rn "RECORD_AUDIO" --include=*.kt app/src/main` returns 0 |

---

## 10. The previous `AGENTS.md` was wrong

**FACT:** The previous `AGENTS.md` at the repo root made claims that the code
contradicts. Source: `AGENTS.md:2`, `:5`, `:8`, `:30`, `:31`.

| Claim in the previous `AGENTS.md` | Reality | Evidence |
|---|---|---|
| "Room" in the tech stack (`:2`) | No Room at all | `grep -rn "androidx.room"` returns 0 |
| "ViewModel" in the data flow (`:5`) | Only one ViewModel exists (`ChatTranscriptStore`) | `grep -rn "class .*: ViewModel()"` returns 1 |
| "Never edit existing exported schema JSONs (`app/schemas/`)" (`:30`) | `app/schemas/` does not exist | `ls app/schemas` fails |
| "Do NOT add or upgrade libraries in `gradle/libs.versions.toml`" (`:31`) | That file does not exist | `ls gradle/libs.versions.toml` fails |
| "passing `MigrationTest.kt`" (`:30`) | No such file | `find . -name "MigrationTest*"` returns nothing |
| KSP implied by Room | No KSP, no KAPT | `grep -rn "ksp\|kapt" --include=*.kts` returns 0 |
| "Run from `android/` root" (`:8`) | There is no `android/` directory | `ls -d android` fails |
| First line is `android/AGENTS.md` (`:1`) | This file is at the repo root | `ls AGENTS.md` |

**FACT:** `CLAUDE.md` is a single line, `@AGENTS.md`. Source: `CLAUDE.md:1`.

**FACT:** `.aider.conf.yml` reads `AGENTS.md`. Source: `.aider.conf.yml:1`.

**FACT:** `.gemini/settings.json` sets `AGENTS.md` as the context file.
Source: `.gemini/settings.json:3`.

**Consequence:** every AI agent that reads this repo was fed eight false claims
and would look for files that do not exist. This file replaces them with
verified facts.

---

## 11. Open items for Phase 2

- `ASSUMPTION`: no linter or formatter is configured for Kotlin beyond
  `kotlin.code.style=official` at `gradle.properties:13`.
- **FACT:** `org.gradle.configuration-cache=true` is on. Source:
  `gradle.properties:7`. Any new Gradle task must be configuration-cache safe.
- **FACT:** `org.gradle.parallel=true` and `org.gradle.caching=true` are on.
  Source: `gradle.properties:5-6`.
- **FACT:** `android.nonTransitiveRClass=true`. Source: `gradle.properties:16`.
- **FACT:** `org.gradle.tooling.parallel=true`. Source: `gradle.properties:19`.
- **FACT:** `org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
  -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8`. Source:
  `gradle.properties:2`.
