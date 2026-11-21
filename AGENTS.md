android/AGENTS.md
Scope: Android app only (Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose). Read root AGENTS.md first for repo-wide guardrails.
1. Tech Stack & Core Architecture
UI Toolkit: 100% Jetpack Compose + Material 3 (androidx.compose.material3, Compose-first). Do NOT create XML UI layouts (themes.xml is only for the Android launch window theme).
State & Data Flow: Unidirectional flow only: UI Screen → ViewModel → Repository → Room DAO / HTTP API.
Strict Separation: Never place business logic, networking, database queries, or API keys inside Composable screens.
No Figma Dependency: Iterate UI directly in Jetpack Compose using @Preview (Light + Dark) and physical device verification.
2. Exact Commands (Run from android/ root)
Build Debug APK: ./gradlew assembleDebug
JVM Unit Tests: ./gradlew testDebugUnitTest
Android Lint: ./gradlew lintDebug
Instrumented / Compose UI Tests (device/emulator): ./gradlew connectedDebugAndroidTest
3. Material 3 & Jetpack Compose UI Rules (Mandatory)
Zero Hardcoded Colors in Screens: Never write Color(0xFF...), Color.White, Color.Black, or Color.Gray in screen composables. Always use MaterialTheme.colorScheme.* or semantic theme tokens (ui/theme/Color.kt, Theme.kt, Shape.kt, Type.kt).
Strict 4.dp Grid: All spacing, padding, sizes, and corner radii must be multiples of 4.dp (only 0.5.dp, 1.dp, 2.dp allowed for hairlines/borders/dividers).
48dp Minimum Touch Targets: Every interactive IconButton, icon action, and clickable row must keep at least 48.dp × 48.dp hit area. Never shrink an IconButton with Modifier.size(20.dp) or similar.
Accessibility & Semantics:
Group radio rows with Modifier.selectableGroup(), use Modifier.selectable(selected, role = Role.RadioButton, onClick = ...) on the row, and set RadioButton(onClick = null) to avoid duplicate TalkBack focus.
Set meaningful stringResource(...) contentDescription on every interactive icon (null only on decorative icons paired with visible text).
M3 1.5+ Expressive API Parity:
Use the modern ListItem(content = ..., supportingContent = ...) overload and ListItemDefaults.colors(contentColor = ..., supportingContentColor = ...) — never deprecated headlineContent / headlineColor.
Anchor DropdownMenu inside a Box wrapping its trigger IconButton (never at screen root).
Wire TopAppBarDefaults.pinnedScrollBehavior() + Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) on scrollable screens.
Typography & Bundled Fonts: Use MaterialTheme.typography.* (including *Emphasized styles) backed by bundled Lato TTF weights (400, 500, 700, 900). Do NOT use FontWeight.SemiBold (600) unless a matching TTF is bundled.
Strings & Localization: Put all user-facing UI text in app/src/main/res/values/strings.xml — zero hardcoded UI copy in composables.
Edge-to-Edge & Safe Activity Lookup: Consume Scaffold innerPadding and apply navigationBarsPadding() + imePadding() on bottom composers/sheets. Safely unwrap Activity via ContextWrapper (never raw view.context as Activity).
UI Design Freeze: When working on ViewModel, Repository, Room, or API tasks, do NOT modify UI colors, typography, spacing, shapes, or navigation unless explicitly requested.
4. Zero-Duplication & Database Guardrails
In-Place Edits Only: Search existing files first and update canonical files in-place. Never create duplicate files (*V2.kt, *New.kt, *Copy.kt) or unrequested helper files.
Room Schema Safety: Never edit existing exported schema JSONs (app/schemas/). Any entity change requires a proper Migration, version bump, and passing MigrationTest.kt.
Dependencies: Do NOT add or upgrade libraries in gradle/libs.versions.toml or build.gradle.kts without explicit user approval.
5. Agent Workflow, Reporting & Git Rules
Autonomous Execution (Non-Coder Friendly): Execute all code edits, Gradle commands, test runs, and root-cause fixes autonomously. Never ask the user to manually edit code or read raw stacktraces. Report results in simple Hinglish.
Ambiguity Gate: If requirements or UI/architecture choices are unclear, STOP before editing, present 4 distinct options + 1 clear recommendation, and wait for approval.
No Skipped Tests: Never mark failing tests @Ignore or delete assertions to force a green build.
Branch & PR Hygiene: After verification passes and secrets are checked (git status / git diff), push changes to a clean feat/... or fix/... branch and open a Pull Request with a simple Hinglish summary. Always ask for approval before merging PRs or deleting branches