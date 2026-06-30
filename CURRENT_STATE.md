# CURRENT_STATE - autonomous execution handoff

**Branch:** `v2.0-wip` (local-only, no upstream)
**Latest baseline commit:** `63b3110` (+ pending commit adding SBOM + Phase-9 verification report)

## Commit policy

- No `Co-Authored-By` trailer.
- Commits use `GurucharanSavanth <savanthgc@gmail.com>`.
- Leave unrelated local edits alone (`.gitignore`, `.idea/appInsightsSettings.xml` are currently modified outside this task).

## Current verified state

- Android app: minSdk 33, targetSdk 36, compileSdk 36.1, versionCode 2, versionName 2.0.
- UI: Compose screens hosted in the existing Fragment/XML navigation shell; `MainActivity` still owns ViewBinding shell.
- State: legacy `MainViewModel` remains LiveData/SavedStateHandle; `HistoryViewModel` uses StateFlow/Flow.
- Persistence: Room v2 append-only History schema is wired; settings/profile state still uses SavedStateHandle + SharedPreferences. DataStore is staged only.
- History: read screen, dashboard "Save readings", and calculator "Log as administered" are wired.
- License: Apache-2.0 `LICENSE` and `NOTICE` exist.

## Latest verification in this workstream

- `./gradlew testDebugUnitTest` passed; generated JVM report shows 138 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew lintDebug` passed.
- `./gradlew assembleDebug` passed.
- `./gradlew testDebugUnitTest assembleDebugAndroidTest lintDebug` passed after the AndroidX Test dependency bump.
- `connectedDebugAndroidTest` was not run because `adb` is not on PATH in this shell.
- `assembleRelease` rerun 2026-06-30: **BUILD SUCCESSFUL in 1m 56s** (R8 minify + resource shrink). APK `app/build/outputs/apk/release/SeachemDosing-v2.0-release.apk` (~16.3 MB) regenerated and current.
- Calculation parity ("sync invariant") verified 2026-06-30: all 17 shared coefficients identical Kotlin↔JS; no coefficient redefinition outside `Calculations.kt`. See `PHASE9_VERIFICATION.md`.
- Static security scan 2026-06-30: zero permissions, no committed secrets, single required LAUNCHER export, `allowBackup=false`. Clean. See `PHASE9_VERIFICATION.md`.

## Known blockers / risks

- Documentation truth audit findings from this pass are corrected; see
  `docs/audit/DOCUMENTATION_TRUTH_AUDIT.md`.
- Compose instrumented UI tests are not CI-gated; prior API-36 Espresso runs hit `InputManager.getInstance`.
- Web v2 is a Vite/TypeScript scaffold and partial engine port, not a complete PWA replacement for `Base_Template/`.
- SBOM now exists: `docs/sbom/SBOM.md` (direct-dependency level; upgrade path to CycloneDX noted in-file).

## Next executable task

Phase 9 static verification (security, calc parity) and the Phase 10 release-build
gate are GREEN (`PHASE9_VERIFICATION.md`). Remaining work is device-tooling-blocked
or owner-gated:
- Run `connectedDebugAndroidTest` / Compose UI tests + emulator performance once
  `adb`/an AVD is available (prior API-36 Espresso `InputManager.getInstance` blocker).
- Validate the uncommitted web `package.json` major bumps (Vite 8 / Vitest 4) against
  the `web/` build before committing them.
- Optional cosmetic: mirror the two Kotlin invalid-input guards into the JS
  (CALC-1/CALC-2 in `PHASE9_VERIFICATION.md`).
