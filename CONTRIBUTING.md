# Contributing to teamily-android-ui

How a human works in this repository. The rules for an AI agent are in [`AGENTS.md`](AGENTS.md);
this file is the human workflow around them. Where the two overlap, `AGENTS.md` wins.

Read [`AGENTS.md`](AGENTS.md) first. It is the rulebook, and `CLAUDE.md`, `.aider.conf.yml` and
`.gemini/settings.json` all point at it.

---

## 1. What this repository is

A Kotlin plus Jetpack Compose Android app, one Gradle module `:app`, `applicationId` and `namespace`
`ai.duagenai.design`. It is a UI client for two backends that live in `KRFbrothers/dua-backend`: the
Node API and the Python engine. Both hosts arrive as generated `BuildConfig` fields resolved by
`ApiConfig`, never as a literal in a screen.

There is no backend here, no database, no Room and no migrations. The backend rules live in the
backend repository's `AGENTS.md`.

---

## 2. Setup

Requirements: JDK 21, Android SDK with `platforms;android-37.1` and `build-tools;37.0.0`.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

`gradle.properties` sets `-Xmx4096m` and turns the configuration cache, parallel build and build cache
on, so any new Gradle task must be configuration-cache safe.

There is no Kotlin linter and no formatter beyond `kotlin.code.style=official`. Do not write "lint
passed" unless you actually ran `lintDebug`, and note that CI does not run it.

---

## 3. Branch and commit

- Branch off `main`. Never commit to `main`.
- One branch, one job.
- Commit message: a short imperative subject, then a body that says why.
- Before every push run `git status --short` and `git diff --cached` and read every line. A committed
  secret is reported and rotated, never scrubbed locally.

---

## 4. What CI runs

| Workflow | Trigger | What it does |
|---|---|---|
| `.github/workflows/pr-checks.yml` | `pull_request` to `main` | `./gradlew :app:testDebugUnitTest`, `contents: read` |
| `.github/workflows/debug-apk-release.yml` | `push` to `main`, `workflow_dispatch` | Builds the APK and publishes a GitHub Release with a `debug-apk-<run_number>` tag |

So a pull request is tested before merge, but the APK build and the release happen only after a push
to `main`. That is why a direct push to `main` publishes a release from unreviewed code.

`pr-checks.yml` deliberately does not run `assembleDebug` (the expensive half) and does not run
`lintDebug` (the repo has no lint baseline, so it would fail on pre-existing warnings).

---

## 5. Pull request

A pull request carries a short Hinglish summary, 3 to 6 lines:

1. What changed and why.
2. What you ran, and the result.
3. What you could not run, and why.
4. The risk, in one line.

Review looks for:

- The smallest change that does the job. No drive-by refactor.
- An existing file edited in place. No `*V2.kt`, no `*New.kt`, no `*Copy.kt`.
- No new hardcoded color under `screens/`. Use `MaterialTheme.colorScheme.*` or the tokens in
  `theme/`.
- No new hardcoded string. `app/src/main/res/values/strings.xml` is the one copy source.
- Every interactive element at least 48dp by 48dp.
- No weakened test, no deleted assertion, no `@Ignore`, no loosened expected value.
- No secret, no keystore, no `.env`.
- `AGENTS.md` updated in the same pull request if a rule or a fact in it changed.

---

## 6. Merging and releasing

Merging to `main` needs an explicit owner yes in the pull request thread, every time. A merge to
`main` triggers the release workflow, so the merge is also a release decision.

---

## 7. Frozen and generated

| Path | Rule |
|---|---|
| `documents/M3-design_v01/` | Frozen design archive. Do not edit it and do not copy a file out of it. A new design goes to `M3-design_v02/` |
| `app/build/`, `.gradle/` | Generated. Never commit |
| `debug-apk-<n>` tags | Created by CI. Never create one by hand |

---

## 8. Where a fact lives

| Fact | Home |
|---|---|
| AI and human rules | `AGENTS.md` |
| HTTP shapes | `KRFbrothers/dua-backend` `API_CONTRACT.md` |
| Architecture | `KRFbrothers/dua-backend` `docs/ARCHITECTURE.md` |
| Approved roadmap | `KRFbrothers/dua-backend` `docs/MASTER_PLAN.md` |

Do not restate a backend shape in a comment here. Link it.

---

## 9. License

See [`LICENSE`](LICENSE).
