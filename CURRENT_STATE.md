# CURRENT_STATE - autonomous execution handoff

**Branch:** `v2.0-wip` (local-only, no upstream)
**Latest completed documentation correction commit before this update:** `996c71f`

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
- Release build was not rerun in the current docs-remediation workstream. Existing generated artifact path is `app/build/outputs/apk/release/SeachemDosing-v2.0-release.apk`, but treat it as historical until `assembleRelease` is rerun.

## Known blockers / risks

- Documentation drift remains the active Phase 9 task, but the high-severity README,
  ADR-003, ADR-008, ADR-011, QA, testing, security, and web-status claims have been
  corrected; see `docs/audit/DOCUMENTATION_TRUTH_AUDIT.md`.
- Compose instrumented UI tests are not CI-gated; prior API-36 Espresso runs hit `InputManager.getInstance`.
- Web v2 is a Vite/TypeScript scaffold and partial engine port, not a complete PWA replacement for `Base_Template/`.
- No SBOM artifact exists yet.

## Next executable task

Continue medium/low documentation remediation from `docs/audit/DOCUMENTATION_TRUTH_AUDIT.md`:
ADR-002 hybrid DI state, dependency catalog comments/Material alias, Tool failure log
status, and any remaining release/compliance wording. Run `git diff --check` and the
smallest relevant verification before committing.
