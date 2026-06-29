# Phase Matrix

**Date:** 2026-06-29
**Current correction baseline before latest update:** `5ff8808`
**Evidence basis:** `SPEC.md`, `MODERNIZATION_PLAN.md`, `CURRENT_STATE.md`, commit history, Gradle config, source/test inventory, generated `testDebugUnitTest` reports, and `DOCUMENTATION_TRUTH_AUDIT.md`.

## Matrix

| Phase | Planned | Implemented | Tested | Documented | Remaining |
|---|---|---|---|---|---|
| 0. Baseline / checkpoint | Preserve WIP, verify baseline, keep master untouched. | Done historically; branch `v2.0-wip` active; latest checkpoint commits present. | Current turn: `testDebugUnitTest`, `lintDebug`, `assembleDebug` passed. | Partially stale: old counts and commit refs remain in `CURRENT_STATE.md`/QA docs. | Refresh state docs after each corrective commit. |
| 1. SDK 33 + cleanup | minSdk 33, Android 13+ docs, dead pre-33 cleanup. | Done: `minSdk = 33`, `targetSdk = 36`, `compileSdk 36.1`; ADR-006. | Unit/lint/debug build passed after README correction. | Mostly current after `f371643` and `756d944`. | No code task pending; keep device-coverage caveat. |
| 2-3. Result model + engines | Sealed result model, validation/unit engines, BigDecimal-safe dosing wrappers. | Partial: `CalcResult`, `ValidationEngine`, `UnitConversionEngine`, `FertilizerChemistryEngine`, recommendation and medication safety/search exist. `DosingResult` is still 2-case; no `DosingCalculationEngine` wrapper. | JVM tests cover result, validation, unit conversion, fertilizer, recommendation, medication safety/search. Latest generated JVM report: 138 tests, 0 fail/error/skip. | SPEC still marks T4/T6 incomplete. | Implement T4/T6 only if product flow needs it; current app still uses `MainViewModel` + `SeachemCalculations` directly. |
| 4-5. Compose UI migration | Compose screens, Material 3 theme, parity, a11y, UI tests. | Screens are Compose-hosted in Fragment/XML shell; History route added; AI orphan removed; theme tokens moved. `MainViewModel` LiveData remains. | JVM tests cover VM/history/dose logging/theme; instrumented tests exist but current API-36 connected run is blocked by missing `adb`. | Design docs refreshed for History/AI state; detailed a11y/responsive/design-system findings remain. | Run compatible instrumented tests when device tooling exists; StateFlow/per-feature VM migration remains a larger implementation phase. |
| 6. Medication module | Evidence-grounded catalog, safety gates, beginner/expert flows. | Partial: Kotlin in-code catalog, safety engine, search engine, Compose screen. Non-Seachem entries remain SECONDARY/verify-label. | Medication safety/search JVM tests exist. | Research report and schema docs exist but some claims are pending/stale. | Verify task #14 product evidence; expand permutation tests/rule tables only after source data is upgraded. |
| 7. Fertilizer module | DIY chemistry engine, stock/target solvers, macro/micro separation, UI. | Partial: molar/ppm engine and Compose screen exist. No stock-solution or nutrient-target solver found. | Fertilizer chemistry JVM tests exist. | Research/schema docs mark solubility/CSM+B pending. | Implement T8 if needed; verify solubility/CSM+B before richer dosing claims. |
| 8. Catalog/search/indexing | Schema-valid catalog, no duplicate IDs, FW/SW separation, richer seed. | Partial: medication search and in-code catalog exist; no Room/JSON seed. | Duplicate/search/safety tests exist. | `PRODUCT_CATALOG_SCHEMA.md` describes current in-code schemas. | Decide whether catalog size justifies Room/JSON; keep SECONDARY evidence warnings. |
| 9. QA / docs / security / accessibility / performance | Drift check, doc truth, security guidance, a11y, performance, compliance. | Docs truth audit findings corrected; AndroidX Test dependencies bumped for retest; security and notices exist. | Unit/lint/debug build passed; `assembleDebugAndroidTest` passed after dependency bump. `connectedDebugAndroidTest` not run because `adb` is unavailable. | Current audit and state docs updated. | Run static security/privacy checks, accessibility review, and emulator/performance checks when `adb` exists. |
| 10. Release readiness | Release build, signing boundary, SBOM/notices, final clean tree. | Partial: release APK artifact exists for v2.0; signing is conditional; Apache-2.0 license and notices exist. | Current turn did not rerun `assembleRelease`; CI does not run release build. | Release docs stale/overstated; no SBOM artifact found. | Rerun release build after doc/code fixes; add SBOM or document absence; final tree must exclude unrelated local edits. |

## Next Executable Phase

`Phase 9: verification` is next. Documentation truth remediation is complete for
the audit findings; remaining Phase 9 work is static security/privacy review,
accessibility review, and emulator/performance evidence when `adb` is available.

## Handoff

**Completed work:** phase matrix built from current evidence; documentation truth remediation completed.
**Active risks:** implementation gaps remain T4/T6/T8/T9/T10/T11/T12/T13; `adb` is unavailable; release/SBOM work remains.
**Tests already passed this turn:** `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`, `./gradlew testDebugUnitTest assembleDebugAndroidTest lintDebug`.
**Verification:** `git diff --check` clean aside from unrelated CRLF warnings in `.gitignore` and `.idea/appInsightsSettings.xml`.
