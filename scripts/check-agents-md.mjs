#!/usr/bin/env node
// Validates the machine-checkable claims in AGENTS.md against this repo.
//
// Why this exists: AGENTS.md section 6.8 says "Nothing validates this file but
// a human", and that is how the file drifted twice already (a stale
// "no CONTRIBUTING.md" claim, and two counts that no longer matched the code).
// This script turns every claim that a machine can check into an assertion, so
// a wrong edit fails CI instead of misleading the next agent that reads it.
//
// Usage:
//   node scripts/check-agents-md.mjs            # check, exit 1 on any failure
//   node scripts/check-agents-md.mjs --verbose  # also print passing claims
//
// Exit codes: 0 all checked claims hold, 1 at least one claim is wrong,
// 2 the script itself could not run (missing repo file it needs to read).
//
// A claim that cannot be checked mechanically is reported as SKIP with the
// reason, never silently dropped. If you add a checkable claim to AGENTS.md,
// add it here in the same PR.

import { readFileSync, existsSync, readdirSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const VERBOSE = process.argv.includes("--verbose");

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

function read(rel) {
  return readFileSync(join(ROOT, rel), "utf8");
}

function exists(rel) {
  return existsSync(join(ROOT, rel));
}

// Runs a program with an argument array. Never a shell string.
//
// Why: execSync(cmd) hands the string to a shell, and the shell differs per
// platform. On Windows that is cmd.exe, where single quotes are not quoting
// characters at all, so `grep -rnE 'a|b' .` reaches cmd.exe with a bare `|`
// and gets split into two commands. The same string under /bin/sh or bash
// works, which is why this check passed on Linux CI and failed on Windows.
// execFileSync spawns the program directly with no shell in between, so
// quoting and metacharacters behave identically on Windows, macOS and Linux.
function run(file, args) {
  try {
    return execFileSync(file, args, {
      cwd: ROOT,
      encoding: "utf8",
      stdio: ["ignore", "pipe", "ignore"],
      maxBuffer: 32 * 1024 * 1024,
    });
  } catch (err) {
    // A non-zero exit is a normal answer here (grep exits 1 when it finds
    // nothing), so return whatever was written to stdout instead of throwing.
    return (err && err.stdout ? err.stdout.toString() : "") || "";
  }
}

// Every file under `dir` (relative to ROOT), recursively. Symlinks are not
// followed and .git is skipped, matching `grep -r`.
function walkFiles(dir, { exts = null } = {}) {
  const out = [];
  if (!existsSync(join(ROOT, dir))) return out;
  const walk = (rel) => {
    for (const entry of readdirSync(join(ROOT, rel), { withFileTypes: true })) {
      if (entry.name === ".git") continue;
      const p = `${rel}/${entry.name}`;
      if (entry.isDirectory()) walk(p);
      else if (entry.isFile() && (!exts || exts.some((e) => entry.name.endsWith(e)))) out.push(p);
    }
  };
  walk(dir);
  return out;
}

// Counts lines matching `re` across every file under `dirs`.
// Equivalent to `grep -rnE <pattern> <dirs> | wc -l`.
// `re` must not carry the /g flag: .test() is stateful with it.
function grepLineCount(re, dirs, { exts = null } = {}) {
  let n = 0;
  for (const dir of dirs) {
    for (const f of walkFiles(dir, { exts })) {
      for (const line of readFileSync(join(ROOT, f), "utf8").split("\n")) {
        if (re.test(line)) n += 1;
      }
    }
  }
  return n;
}

// Counts every occurrence of a literal string across every file under `dirs`.
// Equivalent to `grep -roF <literal> <dirs> | wc -l`.
function grepOccurrences(literal, dirs) {
  let n = 0;
  for (const dir of dirs) {
    for (const f of walkFiles(dir)) {
      const src = readFileSync(join(ROOT, f), "utf8");
      let i = 0;
      while ((i = src.indexOf(literal, i)) !== -1) {
        n += 1;
        i += literal.length;
      }
    }
  }
  return n;
}

// Files under `dir` whose content matches `re`. Equivalent to `grep -rlE`.
function grepFiles(re, dir) {
  return walkFiles(dir).filter((f) => re.test(readFileSync(join(ROOT, f), "utf8")));
}

function countFiles(dir, ext) {
  if (!exists(dir)) return 0;
  let n = 0;
  const walk = (d) => {
    for (const entry of readdirSync(join(ROOT, d), { withFileTypes: true })) {
      const p = `${d}/${entry.name}`;
      if (entry.isDirectory()) walk(p);
      else if (entry.name.endsWith(ext)) n += 1;
    }
  };
  walk(dir);
  return n;
}

// ---------------------------------------------------------------------------
// claim registry
// ---------------------------------------------------------------------------

const claims = [];

// section: the AGENTS.md section the claim lives in, so a failure points at it.
// text: the claim, short enough to read in a CI log.
// fn: returns { ok, expected, actual } or { skip: "reason" }.
function claim(section, text, fn) {
  claims.push({ section, text, fn });
}

function eq(expected, actual) {
  return { ok: expected === actual, expected, actual };
}

function has(rel, needle) {
  if (!exists(rel)) return { ok: false, expected: `${rel} contains ${needle}`, actual: `${rel} missing` };
  const found = read(rel).includes(needle);
  return { ok: found, expected: `${rel} contains ${JSON.stringify(needle)}`, actual: found ? "found" : "not found" };
}

function absent(rel) {
  const there = exists(rel);
  return { ok: !there, expected: `${rel} absent`, actual: there ? "present" : "absent" };
}

function present(rel) {
  const there = exists(rel);
  return { ok: there, expected: `${rel} present`, actual: there ? "present" : "absent" };
}

// Reads the number AGENTS.md itself claims, so the script never hardcodes a
// count that then drifts on its own. `pattern` must have one capture group
// holding the digits or an English number word.
const NUMBER_WORDS = {
  one: 1, two: 2, three: 3, four: 4, five: 5, six: 6, seven: 7, eight: 8,
  nine: 9, ten: 10, eleven: 11, twelve: 12,
};

function claimedNumber(pattern) {
  const src = read("AGENTS.md");
  const m = src.match(pattern);
  if (!m) return null;
  const raw = m[1].trim().toLowerCase();
  if (/^\d+$/.test(raw)) return Number(raw);
  if (raw in NUMBER_WORDS) return NUMBER_WORDS[raw];
  return null;
}

// Compares a number written in AGENTS.md against the repo's real value.
// `label` names the claim for the log; `pattern` finds the claimed digits.
function numberClaim(label, pattern, actualFn) {
  const claimed = claimedNumber(pattern);
  if (claimed === null) {
    return {
      ok: false,
      expected: `AGENTS.md states ${label}`,
      actual: "the sentence was not found in AGENTS.md, so the claim cannot be checked",
    };
  }
  const actual = actualFn();
  return {
    ok: claimed === actual,
    expected: `AGENTS.md says ${claimed}`,
    actual: `${actual} in the repo`,
  };
}

// ---------------------------------------------------------------------------
// Section 1: Shape
// ---------------------------------------------------------------------------

claim("1", 'rootProject.name = "DuaGenAI" in settings.gradle.kts', () =>
  has("settings.gradle.kts", 'rootProject.name = "DuaGenAI"'),
);

claim("1", "one Gradle module, :app, and no other include()", () => {
  const src = read("settings.gradle.kts");
  const includes = [...src.matchAll(/^\s*include\(([^)]*)\)/gm)].map((m) =>
    m[1].trim().replace(/["']/g, ""),
  );
  return eq(":app", includes.join(" | "));
});

claim("1", "no android/ directory at the repo root", () => absent("android"));

claim("1", "applicationId is ai.duagenai.design", () =>
  has("app/build.gradle.kts", 'applicationId = "ai.duagenai.design"'),
);

claim("1", "namespace is ai.duagenai.design", () =>
  has("app/build.gradle.kts", 'namespace = "ai.duagenai.design"'),
);

claim("1", "Kotlin file count under app/src/main/java matches AGENTS.md", () =>
  numberClaim("the Kotlin file count", /(\d+) Kotlin files sit under `app\/src\/main\/java`/, () =>
    countFiles("app/src/main/java", ".kt"),
  ),
);

claim("1", "screens/ Kotlin file count matches AGENTS.md", () =>
  numberClaim("the screens/ count", /(\d+) in `screens\/`/, () =>
    countFiles("app/src/main/java/ai/duagenai/design/screens", ".kt"),
  ),
);

claim("1", "theme/ Kotlin file count matches AGENTS.md", () =>
  numberClaim("the theme/ count", /(\d+) in `theme\/`/, () =>
    countFiles("app/src/main/java/ai/duagenai/design/theme", ".kt"),
  ),
);

claim("1", "README.md is one line", () => {
  const lines = read("README.md").replace(/\n$/, "").split("\n").length;
  return eq(1, lines);
});

claim("1", "documents/M3-design_v01/ is tracked in git", () => {
  const n = run("git", ["ls-files", "documents/M3-design_v01"])
    .split("\n")
    .filter((l) => l.trim()).length;
  return { ok: n > 0, expected: "at least 1 tracked file", actual: `${n} tracked files` };
});

claim("1", "no gradle/libs.versions.toml", () => absent("gradle/libs.versions.toml"));
claim("1", "no app/schemas/", () => absent("app/schemas"));
claim("1", "no supabase/", () => absent("supabase"));
claim("1", "no webpages/", () => absent("webpages"));
claim("1", "no Dependabot config", () => {
  const a = exists(".github/dependabot.yml");
  const b = exists(".github/dependabot.yaml");
  return { ok: !a && !b, expected: "no .github/dependabot.yml or .yaml", actual: a || b ? "present" : "absent" };
});

claim("1", "no Room, no KSP, no KAPT, no MigrationTest", () => {
  const hits = grepLineCount(
    /androidx\.room|\bksp\b|\bkapt\b|MigrationTest/,
    ["."],
    { exts: [".kts", ".kt", ".properties"] },
  );
  return { ok: hits === 0, expected: "0 matches", actual: `${hits} matches` };
});

claim("1", "pr-checks.yml has a pull_request trigger on main", () => {
  const src = read(".github/workflows/pr-checks.yml");
  const ok = /on:\s*\n\s*pull_request:/.test(src) && /branches:\s*\[main\]/.test(src);
  return { ok, expected: "on: pull_request: branches: [main]", actual: ok ? "present" : "missing" };
});

claim("1", "pr-checks.yml runs testDebugUnitTest only, not assembleDebug or lintDebug", () => {
  // Only the `run:` bodies count. The file's header comments mention
  // assembleDebug and lintDebug on purpose, explaining why they are absent.
  const src = read(".github/workflows/pr-checks.yml");
  const runBodies = [...src.matchAll(/^\s+run:\s*(.*)$/gm)].map((m) => m[1]).join("\n");
  const runs = /testDebugUnitTest/.test(runBodies);
  const noAssemble = !/assembleDebug/.test(runBodies);
  const noLint = !/lintDebug/.test(runBodies);
  return {
    ok: runs && noAssemble && noLint,
    expected: "testDebugUnitTest yes, assembleDebug no, lintDebug no",
    actual: `testDebugUnitTest=${runs} assembleDebug=${!noAssemble} lintDebug=${!noLint}`,
  };
});

claim("1", "CONTRIBUTING.md exists at the repo root", () => present("CONTRIBUTING.md"));

claim("1", "CLAUDE.md, .aider.conf.yml and .gemini/settings.json all point at AGENTS.md", () => {
  const files = ["CLAUDE.md", ".aider.conf.yml", ".gemini/settings.json"];
  const missing = files.filter((f) => !exists(f) || !read(f).includes("AGENTS.md"));
  return {
    ok: missing.length === 0,
    expected: "all three mention AGENTS.md",
    actual: missing.length === 0 ? "all three mention it" : `missing in ${missing.join(", ")}`,
  };
});

// ---------------------------------------------------------------------------
// Section 2: Toolchain
// ---------------------------------------------------------------------------

claim("2", "AGP 9.2.0", () => has("build.gradle.kts", 'id("com.android.application") version "9.2.0"'));
claim("2", "Kotlin Compose plugin 2.2.21", () =>
  has("build.gradle.kts", 'id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"'),
);
claim("2", "Gradle wrapper 9.6.0", () =>
  has("gradle/wrapper/gradle-wrapper.properties", "gradle-9.6.0-bin.zip"),
);
claim("2", "compileSdk 37 with minorApiLevel 1", () => {
  const src = read("app/build.gradle.kts");
  const ok = /compileSdk\s*\{[\s\S]*?version\s*=\s*release\(37\)[\s\S]*?minorApiLevel\s*=\s*1/.test(src);
  return { ok, expected: "release(37) with minorApiLevel = 1", actual: ok ? "present" : "missing" };
});
claim("2", "targetSdk 36", () => has("app/build.gradle.kts", "targetSdk = 36"));
claim("2", "minSdk 24", () => has("app/build.gradle.kts", "minSdk = 24"));
claim("2", "versionCode 1", () => has("app/build.gradle.kts", "versionCode = 1"));
claim("2", 'versionName "1.0"', () => has("app/build.gradle.kts", 'versionName = "1.0"'));
claim("2", "sourceCompatibility 17", () =>
  has("app/build.gradle.kts", "sourceCompatibility = JavaVersion.VERSION_17"),
);
claim("2", "targetCompatibility 17", () =>
  has("app/build.gradle.kts", "targetCompatibility = JavaVersion.VERSION_17"),
);
claim("2", "jvmTarget JVM_17", () => has("app/build.gradle.kts", "JvmTarget.JVM_17"));
claim("2", "the Kotlin Android plugin is not applied", () => {
  const src = read("app/build.gradle.kts");
  const ok = !/org\.jetbrains\.kotlin\.android/.test(src);
  return { ok, expected: "no org.jetbrains.kotlin.android plugin", actual: ok ? "absent" : "applied" };
});
claim("2", "android.suppressUnsupportedCompileSdk=37.1 in gradle.properties", () =>
  has("gradle.properties", "android.suppressUnsupportedCompileSdk=37.1"),
);
claim("2", "core-ktx 1.13.1", () => has("app/build.gradle.kts", "androidx.core:core-ktx:1.13.1"));
claim("2", "activity-compose 1.9.0", () =>
  has("app/build.gradle.kts", "androidx.activity:activity-compose:1.9.0"),
);
claim("2", "Compose BOM 2026.11.01", () =>
  has("app/build.gradle.kts", "androidx.compose:compose-bom:2026.11.01"),
);
claim("2", "isMinifyEnabled is false", () => has("app/build.gradle.kts", "isMinifyEnabled = false"));
claim("2", "no signingConfig is declared", () => {
  const src = read("app/build.gradle.kts");
  const ok = !/signingConfig/.test(src);
  return { ok, expected: "no signingConfig", actual: ok ? "absent" : "declared" };
});
claim("2", "gradle.properties sets -Xmx4096m", () => has("gradle.properties", "-Xmx4096m"));
claim("2", "configuration cache is on", () => has("gradle.properties", "org.gradle.configuration-cache=true"));
claim("2", "parallel build is on", () => has("gradle.properties", "org.gradle.parallel=true"));
claim("2", "build cache is on", () => has("gradle.properties", "org.gradle.caching=true"));

// ---------------------------------------------------------------------------
// Section 3: Commands and CI
// ---------------------------------------------------------------------------

claim("3", 'debug-apk-release.yml is named "Debug APK Release"', () =>
  has(".github/workflows/debug-apk-release.yml", "name: Debug APK Release"),
);
claim("3", "debug-apk-release.yml runs testDebugUnitTest and assembleDebug", () => {
  const src = read(".github/workflows/debug-apk-release.yml");
  const ok = /testDebugUnitTest/.test(src) && /assembleDebug/.test(src);
  return { ok, expected: "both tasks present", actual: ok ? "both present" : "one missing" };
});
claim("3", "debug-apk-release.yml runs on push to main and on workflow_dispatch", () => {
  const src = read(".github/workflows/debug-apk-release.yml");
  const ok = /push:/.test(src) && /branches:\s*\[main\]/.test(src) && /workflow_dispatch:/.test(src);
  return { ok, expected: "push to main plus workflow_dispatch", actual: ok ? "present" : "missing" };
});
claim("3", "debug-apk-release.yml has contents: write", () =>
  has(".github/workflows/debug-apk-release.yml", "contents: write"),
);
claim("3", "debug-apk-release.yml never runs lintDebug, assembleRelease or connectedDebugAndroidTest", () => {
  const src = read(".github/workflows/debug-apk-release.yml");
  const bad = ["lintDebug", "assembleRelease", "connectedDebugAndroidTest"].filter((t) => src.includes(t));
  return { ok: bad.length === 0, expected: "none of the three", actual: bad.length ? bad.join(", ") : "none" };
});
claim("3", "pr-checks.yml has contents: read", () => has(".github/workflows/pr-checks.yml", "contents: read"));

// ---------------------------------------------------------------------------
// Section 4: What must never be committed
// ---------------------------------------------------------------------------

claim("4", ".gitignore blocks the listed build and secret paths", () => {
  const src = read(".gitignore");
  const needed = [".gradle/", "build/", "local.properties", "*.jks", "*.keystore", ".env", "node_modules/"];
  const missing = needed.filter((p) => !src.includes(p));
  return {
    ok: missing.length === 0,
    expected: "all listed patterns present",
    actual: missing.length ? `missing ${missing.join(", ")}` : "all present",
  };
});

claim("4", "no keystore, .env or signing file anywhere in git history", () => {
  // Every path ever added anywhere in history, deduplicated, then counted
  // against the secret-file pattern. This replaces
  // `git log ... | sort -u | grep -icE ...`, whose real pipes were the other
  // half of the Windows problem: cmd.exe would have run them, but the quoted
  // alternation inside the pattern would still have been mangled.
  const added = run("git", [
    "log",
    "--all",
    "--pretty=format:",
    "--name-only",
    "--diff-filter=A",
  ]);
  const unique = [...new Set(added.split("\n").map((l) => l.trim()).filter(Boolean))];
  const secret = /keystore|\.jks|\.keystore|\.env$|\.env\.|signing|\.p12|\.pem/i;
  const count = unique.filter((p) => secret.test(p)).length;
  return { ok: count === 0, expected: "0", actual: String(count) };
});

// ---------------------------------------------------------------------------
// Section 6: Rules
// ---------------------------------------------------------------------------

claim("6.4", "the workflow count matches AGENTS.md", () =>
  numberClaim("the workflow count", /`FACT:` (\w+) workflows exist/, () =>
    readdirSync(join(ROOT, ".github/workflows")).filter((f) => /\.ya?ml$/.test(f)).length,
  ),
);

claim("6.12", "0 hardcoded colors under screens/", () => {
  const n = grepLineCount(
    /Color\(0x|Color\.White|Color\.Black|Color\.Gray/,
    ["app/src/main/java/ai/duagenai/design/screens"],
  );
  return eq(0, n);
});

claim("6.13", "TouchTargets.kt exists", () =>
  present("app/src/main/java/ai/duagenai/design/screens/TouchTargets.kt"),
);

claim("6.14", "strings.xml is the one copy source", () =>
  present("app/src/main/res/values/strings.xml"),
);

claim("6.14", "text literal count in screens/ matches AGENTS.md", () =>
  numberClaim("the text literal count", /(\d+) `text = "\.\.\."` literals remain in `screens\/`/, () =>
    grepOccurrences('text = "', ["app/src/main/java/ai/duagenai/design/screens"]),
  ),
);

claim("6.16", "no Room, no schema directory, no migration file, no SQL", () => {
  const sql = countFiles("app/src", ".sql");
  const room = grepLineCount(/androidx\.room/, ["app"], { exts: [".kt", ".kts"] });
  return {
    ok: sql === 0 && room === 0 && !exists("app/schemas"),
    expected: "0 SQL files, 0 Room refs, no app/schemas",
    actual: `${sql} SQL, ${room} Room refs, schemas ${exists("app/schemas") ? "present" : "absent"}`,
  };
});

claim("6.17", "DuaAgentSkills hardcodes the number of starter skills AGENTS.md claims", () =>
  numberClaim("the starter skill count", /hardcodes (\w+) starter skills/, () => {
    const src = read("app/src/main/java/ai/duagenai/design/screens/ComposerSheets.kt");
    const block = src.slice(src.indexOf("object DuaAgentSkills"));
    const body = block.slice(0, block.indexOf("val ids"));
    return (body.match(/DuaAgentSkill\(/g) || []).length;
  }),
);

claim("6.17", "BaseRemoteRepository maps personal-agent to a display name", () =>
  has("app/src/main/java/ai/duagenai/design/screens/BaseRemoteRepository.kt", '"personal-agent" -> "Personal Agent"'),
);

claim("6.17", "BaseRemoteRepository blocks delete for personal-agent and dua-agent", () =>
  has(
    "app/src/main/java/ai/duagenai/design/screens/BaseRemoteRepository.kt",
    'if (id == "personal-agent" || id == "dua-agent")',
  ),
);

claim("6.17", "InMemorySkillsDataSource seeds every skill with isStarter = true", () =>
  has("app/src/main/java/ai/duagenai/design/screens/BaseRemoteRepository.kt", "isStarter = true"),
);

claim("6.17", "HomeScreen keeps PERSONAL_AGENT_TITLE as the pinned hero card", () =>
  has("app/src/main/java/ai/duagenai/design/screens/HomeScreen.kt", "PERSONAL_AGENT_TITLE"),
);

claim("6.17", "personal-agent id and display-name counts match AGENTS.md", () => {
  const src = read("AGENTS.md");
  const m = src.match(/(\d+) matches for the literal `personal-agent` id across `app\/src`, and (\d+) for the display-name forms/);
  if (!m) {
    return { ok: false, expected: "AGENTS.md states both counts", actual: "sentence not found" };
  }
  const claimedId = Number(m[1]);
  const claimedName = Number(m[2]);
  const actualId = grepOccurrences("personal-agent", ["app/src"]);
  const actualName =
    grepOccurrences("Personal Agent", ["app/src"]) +
    grepOccurrences("PERSONAL_AGENT", ["app/src"]);
  return {
    ok: claimedId === actualId && claimedName === actualName,
    expected: `AGENTS.md says id=${claimedId}, display-name=${claimedName}`,
    actual: `id=${actualId}, display-name=${actualName}`,
  };
});

// ---------------------------------------------------------------------------
// Section 7: Cross-repo
// ---------------------------------------------------------------------------

claim("7", "ChatTranscriptStore.kt cites sse.mjs", () =>
  has("app/src/main/java/ai/duagenai/design/screens/ChatTranscriptStore.kt", "sse.mjs"),
);
claim("7", "ChatTranscriptStoreTest.kt cites sse.mjs", () =>
  has("app/src/test/java/ai/duagenai/design/screens/ChatTranscriptStoreTest.kt", "sse.mjs"),
);
claim("7", "X-Dua-Native: 1 is sent from JsonHttpClient", () =>
  has("app/src/main/java/ai/duagenai/design/screens/JsonHttpClient.kt", '"X-Dua-Native", "1"'),
);
claim("7", "instanceFollowRedirects is false on authenticated requests", () =>
  has("app/src/main/java/ai/duagenai/design/screens/JsonHttpClient.kt", "instanceFollowRedirects = false"),
);

// ---------------------------------------------------------------------------
// Section 9: Known defects
// ---------------------------------------------------------------------------

claim("9", "ApiConfig declares sheetSecretKey", () =>
  has("app/src/main/java/ai/duagenai/design/screens/ApiConfig.kt", "sheetSecretKey"),
);
claim("9", "SheetBackendClient still ships that secret", () =>
  has("app/src/main/java/ai/duagenai/design/screens/SheetBackendClient.kt", "ApiConfig.sheetSecretKey"),
);
claim("9", "allowBackup is enabled in AndroidManifest.xml", () =>
  has("app/src/main/AndroidManifest.xml", 'android:allowBackup="true"'),
);
claim("9", "SessionStore stores the token in noBackupFilesDir", () =>
  has("app/src/main/java/ai/duagenai/design/screens/SessionStore.kt", "noBackupFilesDir"),
);
claim("9", "the oversized test class line count matches AGENTS.md", () => {
  const claimed = claimedNumber(/One (\d+)-line test class in `nav\/`/);
  if (claimed === null) {
    return { ok: false, expected: "AGENTS.md states the test class size", actual: "sentence not found" };
  }
  // The claim is about a test class, so only app/src/test counts. The nav
  // package also holds a 1148-line production file, which is not the claim.
  // Replaces `find app/src/test -name '*.kt' -path '*nav*' -exec wc -l {} +`.
  // wc -l counts newline characters, so count those rather than split()
  // length, which would add one for a file with no trailing newline.
  const big = walkFiles("app/src/test", { exts: [".kt"] })
    .filter((p) => p.includes("nav"))
    .map((p) => [String((readFileSync(join(ROOT, p), "utf8").match(/\n/g) || []).length), p])
    .filter(([n]) => Number(n) > 1000)
    .map(([n, p]) => `${n} ${p}`);
  const ok = big.length === 1 && big[0].startsWith(`${claimed} `);
  return {
    ok,
    expected: `AGENTS.md says ${claimed}, and exactly one test file over 1000 lines`,
    actual: big.join(", ") || "no test file over 1000 lines",
  };
});
claim("9", "Robolectric @Config(sdk = [34]) in every test file that uses @Config", () => {
  const files = grepFiles(/@Config/, "app/src/test");
  // One file also passes qualifiers, so match the sdk argument, not the
  // whole annotation.
  const pinned = grepFiles(/@Config\(sdk = \[34\]/, "app/src/test");
  return {
    ok: files.length > 0 && files.length === pinned.length,
    expected: "every test file with @Config pins sdk 34",
    actual: `${files.length} files with @Config, ${pinned.length} pinned to 34`,
  };
});
claim("9", "RECORD_AUDIO is declared but never requested from Kotlin", () => {
  const declared = read("app/src/main/AndroidManifest.xml").includes("android.permission.RECORD_AUDIO");
  const used = grepLineCount(/RECORD_AUDIO/, ["app/src/main/java"]);
  return {
    ok: declared && used === 0,
    expected: "declared in manifest, 0 Kotlin references",
    actual: `declared=${declared}, kotlin refs=${used}`,
  };
});

// ---------------------------------------------------------------------------
// Claims that cannot be checked mechanically, listed so they are not forgotten
// ---------------------------------------------------------------------------

const skipped = [
  ["1", '"no backend" (a statement about what the app is, not a file)', "semantic claim, no file to assert on"],
  ["2", "AGP 9 built-in Kotlin support", "a Gradle behaviour, not a file content"],
  ["3", "a build may be impossible where you run", "depends on the machine, not the repo"],
  ["5", "observed branches are feat/<topic> and fix/<topic>", "git history changes with every merge"],
  ["6.6", "no linter or formatter is configured beyond kotlin.code.style=official", "absence of config is not enumerable"],
  ["6.11", "themes.xml exists only for the launch window theme", "intent, not a checkable fact"],
  ["6.15", "UI design freeze", "a rule for humans, not a repo fact"],
  ["8", "Done checklist", "a process, not a repo fact"],
  ["10", "why this file replaced the last two", "history narrative"],
];

// ---------------------------------------------------------------------------
// run
// ---------------------------------------------------------------------------

let failed = 0;
let passed = 0;
const failures = [];

for (const c of claims) {
  let result;
  try {
    result = c.fn();
  } catch (err) {
    result = { ok: false, expected: "check runs", actual: `threw: ${err.message}` };
  }
  if (result.skip) {
    if (VERBOSE) console.log(`SKIP  [${c.section}] ${c.text} (${result.skip})`);
    continue;
  }
  if (result.ok) {
    passed += 1;
    if (VERBOSE) console.log(`PASS  [${c.section}] ${c.text}`);
  } else {
    failed += 1;
    failures.push({ c, result });
    console.log(`FAIL  [${c.section}] ${c.text}`);
    console.log(`        expected: ${result.expected}`);
    console.log(`        actual:   ${result.actual}`);
  }
}

console.log("");
console.log(
  `AGENTS.md claims: ${passed} passed, ${failed} failed, ${skipped.length} skipped (not machine-checkable)`,
);

if (VERBOSE) {
  console.log("");
  console.log("Skipped claims, with the reason each cannot be checked:");
  for (const [section, text, reason] of skipped) {
    console.log(`  [${section}] ${text} (${reason})`);
  }
}

if (failed > 0) {
  console.log("");
  console.log("Fix AGENTS.md, or fix the code, or update this script if the claim itself changed.");
  console.log("Never delete a check to make it pass: that is the drift this script exists to stop.");
  process.exit(1);
}

process.exit(0);
