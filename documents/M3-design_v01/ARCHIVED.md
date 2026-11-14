# ARCHIVED — do not edit, do not copy from here

This directory is the **original v01 design snapshot**, kept only as a historical
reference. It is **not** the maintained source of the app.

| | |
|---|---|
| **Status** | 🔒 Archived / frozen |
| **Maintained copy** | [`app/src/main/java/ai/duagenai/design/`](../../app/src/main/java/ai/duagenai/design/) |

## Why this note exists

v01 was committed as a design reference and then drifted: the real app under
`app/` received a series of review fixes that were never back-ported here. By the
time the drift was noticed, v01 still contained code that had been fixed
elsewhere — including a **JSON injection** in the two remote data sources.

A stale copy of vulnerable code is a real hazard, not just clutter: the next
person to "restore" a file from the design package would silently reintroduce
the bug. So v01 is both **marked archived** and **patched in place** — the
vulnerable constructs are gone even here.

## What was neutralised in this directory

| File | Was | Now |
|---|---|---|
| `ArtifactsRepository.kt` | `add()` built the POST body by string interpolation | built with `JSONObject` (values escaped, `JSONObject.NULL` for a null url) |
| `ArtifactsRepository.kt` | `delete()` interpolated the id straight into the path | id passed through `encodePathSegment()` |
| `MemoryRepository.kt` | same two problems in `add()` / `delete()` | same two fixes |
| `JsonHttpClient.kt` | no path-segment encoder | `encodePathSegment()` added |

The patches are deliberately minimal and self-contained: they make the archived
files safe to read and safe to copy, without pretending v01 is current. v01 still
lacks every other fix that landed in `app/` (typed `ApiError`, `WriteResult`,
`TokenProvider` wiring, the launcher icon, …) — which is exactly why it is
archived rather than maintained.

## Rule

- **Do not edit files in this directory.** Change `app/` instead.
- **Do not copy files out of this directory into `app/`.** If you need a file
  from the design package, take it from `app/src/main/`.
- If you need to know what v01 originally looked like, use git history:
  `git log -- documents/M3-design_v01/`.
