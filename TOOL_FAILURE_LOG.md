# TOOL_FAILURE_LOG.md

**Project:** Seachem-Dosing v2.0 modernization
**Last updated:** 2026-06-29

Records tools/commands that failed, were unavailable, or whose live state contradicts the prompt's expected inventory. Each entry: exact observation, likely cause, safe remediation attempted, fallback, capability loss, retest status.

---

## F-01 — `agent-skills` (addy-agent-skills): NOT INSTALLED

- **Prompt expectation:** `expected_status="failed_to_load"` — diagnose the load error.
- **Live observation:** Plugin absent from `/plugin list` entirely. Not failed-to-load — simply **not installed** in this environment.
- **Cause:** Never installed (or removed) in this Claude Code profile.
- **Safe remediation attempted:** None — there is no error/log to diagnose and installing a plugin is an owner action. Did **not** modify plugin config.
- **Fallback:** `superpowers` covers the needed skill surface (planning, TDD, debugging, verification).
- **Capability loss:** None material for this project.
- **Retest:** Re-check after owner installs it, if desired.

## F-02 — `mdk-mcp`: NOT A SEPARATE PLUGIN

- **Prompt expectation:** `expected_status="failed_or_enabled"` — diagnose the reported failure.
- **Live observation:** No plugin named `mdk-mcp`. SAP MDK capability is provided by **`sap-mdk-server` 0.4.0, enabled**.
- **Cause:** Prompt inventory drift / naming mismatch.
- **Remediation:** None needed — no failure exists.
- **Fallback / disposition:** Marked **not applicable** (native Android project, no SAP MDK scope). See ledger §1.
- **Capability loss:** None.

## F-03 — `/plan` slash command: UNAVAILABLE

- **Prompt expectation:** run `/plan` after `/spec`.
- **Live observation:** No `/plan` skill or command registered. Present alternatives: `superpowers:writing-plans` skill and the `Plan` subagent.
- **Cause:** Not part of installed plugins.
- **Fallback (recorded substitution):** **`superpowers:writing-plans`** is the primary `/plan` substitute; `Plan` subagent as secondary. MODERNIZATION_PLAN.md will be produced via these.
- **Capability loss:** None — equivalent planning capability available.

## F-04 — Stale governing assumptions (`CLAUDE.md` + prompt premises)

- **Observation:** `CLAUDE.md` and the prompt describe compileSdk 34 / JDK 11 / "Seachem calculator to be modernized." Working tree is already compileSdk 36.1 / targetSdk 36 / JDK 17, with Compose+Koin+Room scaffolding, ADR-001..005, CI, and a `web/` TS rewrite (all were uncommitted).
- **Cause:** Docs/prompt predate the in-progress v2.0 work that was sitting uncommitted.
- **Remediation:** Preserved the WIP on `v2.0-wip @ 3b14d05` (checkpoint); normalized EOL. Treating live build files as ground truth. `CLAUDE.md` is gitignored, so it is not auto-updated; flag for owner.
- **Capability loss:** None — corrected course.

---

## F-05 — Async research subagents: INTERMITTENT (2 of 4 no-op'd)

- **Observation:** 4 async `general-purpose` research agents dispatched. **2 returned 0 tool_uses / junk** in 2–3 s (Android, fertilizer) — no research. **2 ran real research:** Seachem agent = 18 tool_uses / 261 s / full sourced report (corroborated the main thread's own 7 fetches); API/Fritz/Kordon agent = 34 tool_uses / 378 s but its **final report was truncated by the session limit** (see F-06), so its synthesized findings did not return.
- **Cause:** Async agent reliability is inconsistent in this env; some start the research loop, some no-op.
- **Remediation:** Main thread independently fetches + cross-checks all agent output. Verified main-thread WebSearch/WebFetch work (Android + Seachem fetched successfully).
- **Fallback (in effect):** Treat agent output as advisory; main thread is the system of record. Do not rely on async fan-out for completeness.
- **Capability loss:** Reliable parallel research fan-out unavailable.
- **Retest:** When web resets, optionally resume the API/Fritz/Kordon agent to recover its 34-tool-use findings, or just re-fetch the 5 pages directly (preferred).

## F-06 — WebFetch blocked: session limit + opus-4-8 classifier unavailable

- **Observation:** Mid-research, WebFetch returned `You've hit your session limit · resets 5:50pm (Asia/Kolkata)` and, on parallel calls, `claude-opus-4-8 is temporarily unavailable, so auto mode cannot determine the safety of WebFetch`.
- **Impact:** The 5 non-Seachem med product-page **verbatim dose** fetches + planned **PubChem solubility** fetches could not complete.
- **Fallback (in effect):** (a) WS2 non-Seachem facts sourced from WebSearch result *summaries* → graded **SECONDARY**, with the Kordon aquarium-vs-pond dose explicitly flagged needs-verification. (b) WS3 molar masses + nutrient fractions **computed directly** (grade STANDARD, arithmetic shown in report); solubility values marked pending. Read-only/code work continues unaffected.
- **Capability loss:** Temporary; web research only.
- **Retest (after ~17:50 IST):** re-fetch the 5 med pages + their SDS, fetch PubChem solubility, then upgrade grades in DEEP_RESEARCH_REPORT.md. Tracked as task #14.

---

## F-07 — Instrumented Compose UI tests blocked on API-36 emulator (Espresso InputManager)

- **Observation:** `connectedDebugAndroidTest` fails with `NoSuchMethodException: android.hardware.input.InputManager.getInstance` from `Espresso.onIdle` (via Compose `EspressoLink`). Only AVD available is **Pixel_10_Pro_XL (API 36)**.
- **Cause:** Espresso (incl. bumped 3.6.1) reflects on `InputManager.getInstance()`, removed in Android 15/16 (API 35/36). AndroidX Test release notes now list Espresso 3.7.0 and ext.junit 1.3.0 as stable, so the dependency has been bumped for retest.
- **Remediation attempted:** bumped `espresso-core` 3.5.1→3.6.1 + `ext-junit` 1.1.5→1.2.1 — did NOT resolve (same exception; newer stack lines confirm 3.6.1 is active).
- **Fallback (in effect):** verify Compose screens on-device via `installDebug` + `am start` + `screencap` + `adb input tap` (no Espresso). **ProfileSelection confirmed historically**: renders correctly, selection interaction works, no crash, POND→"Sand and Gravel".
- **Capability loss:** automated instrumented UI assertions remain unverified on API 36 until `connectedDebugAndroidTest` is rerun with the upgraded dependencies. JVM unit tests unaffected.
- **Retest/follow-up:** run `connectedDebugAndroidTest` on the API-36 emulator after the 3.7.0/1.3.0 bump; if still blocked, create an API≤34 AVD for instrumented runs. `ProfileSelectionScreenTest` retained.
- **2026-06-29 preflight:** `./gradlew testDebugUnitTest assembleDebugAndroidTest lintDebug` passed after the dependency bump. `connectedDebugAndroidTest` was not run because `adb` is not on PATH in this shell.

## Build/command failures
_Baseline + per-phase builds all green (see commits). No unresolved build failures._
