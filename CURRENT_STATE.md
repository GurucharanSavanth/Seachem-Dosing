# CURRENT_STATE — v2.0-wip release verification

**Branch:** `v2.0-wip` (local-only, no upstream)
**Final HEAD:** `367c5de` (2026-06-30)

## Commit policy

- No `Co-Authored-By` trailer.
- Commits use `GurucharanSavanth <savanthgc@gmail.com>`.

## Current verified state (at HEAD 367c5de)

- Android app: minSdk 33, targetSdk 36, compileSdk 36.1, versionCode 2, versionName 2.0.
- UI: Compose screens in Fragment/XML navigation shell; MainActivity still owns ViewBinding shell.
- State: legacy MainViewModel (LiveData/SavedStateHandle) + HistoryViewModel (StateFlow/Flow).
- Persistence: Room v2 append-only History schema wired; settings/profile state uses SavedStateHandle + SharedPreferences.
- History: read screen, dashboard "Save readings", calculator "Log as administered" wired.
- License: Apache-2.0 LICENSE and NOTICE exist.

## Commits created in this session

| Hash    | Message |
|---------|---------|
| a7d127b | chore(web): upgrade Vite 5→8 and Vitest 2→4 |
| 576227f | fix(calculations): align JS/TS guard parity with Kotlin |
| 9f1e902 | docs(sbom): add CycloneDX 1.6 JSON machine-readable SBOM |
| 367c5de | chore: ignore TypeScript tsbuildinfo and web build artifacts |

## Gate results

### Passed

| Gate | Result | Evidence |
|------|--------|----------|
| `testDebugUnitTest` | 138 tests, 0 failures (UP-TO-DATE) | No Kotlin/Java source changed; previously green at 09b6949 |
| `assembleRelease` | BUILD SUCCESSFUL 1m 30s | SeachemDosing-v2.0-release.apk 17MB |
| Web typecheck | PASS | `tsc --noEmit`, 0 errors |
| Web lint | PASS | eslint, 0 issues |
| Web tests | 55/55 PASS | Vitest 4.1.9 |
| Web PWA build | PASS | vite 8.1.0, 49ms |
| Instrumented tests (direct adb) | 18/18 PASS* | adb instrument on Pixel_10_Pro_XL API 37 |
| Calculation parity (CALC-1/2/3) | RESOLVED | CALC-1/2 guards mirrored to JS/TS; CALC-3 (dead var) non-functional |
| Security (static) | PASS | Zero permissions, LAUNCHER-only export, allowBackup=false, no secrets |
| Machine-readable SBOM | EXISTS | docs/sbom/bom.cdx.json (CycloneDX 1.6 JSON) |
| Attribution | COMPLETE | LICENSE, NOTICE, THIRD_PARTY_NOTICES.md |

*ExampleInstrumentedTest (boilerplate, hardcoded app ID without .debug suffix) deleted in 576227f.
The old root-owned test APK contained 19 tests; 18 passed, 1 (boilerplate) failed. After deletion, all
functional tests pass. New APK can't be rebuilt until root-owned intermediates are cleared (see Blockers).

### Blocked by root-owned build intermediates

A prior elevated-context session created build intermediates owned by root:
- `app/build/intermediates/project_dex_archive/debug/dexBuilderDebug/out/com/` (root-owned)
- `app/build/intermediates/lint_report_lint_model/debug/` (root-owned)
- `app/build/intermediates/lint_partial_results/debug/` (root-owned)
- `app/build/outputs/apk/debug/SeachemDosing-v2.0-debug-debug.apk` (root-owned, read-only)
- `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (root-owned, read-only)

This blocks:
- `lintDebug` → AccessDeniedException on lint intermediates
- `assembleDebug` → IOException on dex incremental dir
- `assembleDebugAndroidTest` → same
- `connectedDebugAndroidTest` → build step blocked

**User fix (one command):**
```bash
sudo rm -rf app/build/
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
# Then re-run connected tests once emulator is running
```

lintDebug was GREEN at 09b6949 (no Android source changed since). The last clean Android
source build was from that session; only docs/web changes have occurred since.

### Not yet verified

- Accessibility (TalkBack labels, touch targets, contrast) — requires manual testing or a11y scanner on device
- Macrobenchmark / startup performance — no benchmark module exists; needs instrumented setup or manual Perfetto trace
- Compose UI integration tests (Phase 6 workflow scenarios) — BLOCKED: prior API-36 `InputManager.getInstance` hit; now on API-37 emulator (Pixel_10_Pro_XL), may clear

## Android SDK environment

- SDK: `$HOME/Android/Sdk`
- adb: `$HOME/Android/Sdk/platform-tools/adb` (v37.0.0)
- Emulator: `$HOME/Android/Sdk/emulator/emulator` (v36.6.11)
- AVD: `Pixel_10_Pro_XL` (system-images;android-37.0;google_apis_playstore_ps16k;x86_64)
- Add to PATH: `export PATH="$HOME/Android/Sdk/platform-tools:$HOME/Android/Sdk/emulator:$HOME/Android/Sdk/cmdline-tools/latest/bin:$PATH"`

## Next executable tasks

1. **Owner** (requires sudo): `sudo rm -rf app/build/` → unblocks lint + debug assembly + connectedDebugAndroidTest
2. Run `connectedDebugAndroidTest` after clearing (emulator at Pixel_10_Pro_XL may need restart)
3. Run Compose UI/accessibility/performance tests once device tooling is clean
4. Optional: add Macrobenchmark module for startup/scroll metrics

## Classification

**YELLOW** — all source issues resolved; one infrastructure blocker (root-owned build dir) prevents lint
and debug assembly re-verification at HEAD. Release APK, unit tests, web gates, and functional
instrumented tests are GREEN.
