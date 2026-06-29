# Phase Matrix

**Date:** 2026-06-29
**Current commit:** `cb19260`
**Evidence basis:** `SPEC.md`, `MODERNIZATION_PLAN.md`, `CURRENT_STATE.md`, commit history, Gradle config, source/test inventory, generated `testDebugUnitTest` reports, and `DOCUMENTATION_TRUTH_AUDIT.md`.

## Matrix

| Phase | Planned | Implemented | Tested | Documented | Remaining |
|---|---|---|---|---|---|
| 0. Baseline / checkpoint | Preserve WIP, verify baseline, keep master untouched. | Done historically; branch `v2.0-wip` active; latest checkpoint commits present. | Current turn: `testDebugUnitTest`, `lintDebug`, `assembleDebug` passed. | Partially stale: old counts and commit refs remain in `CURRENT_STATE.md`/QA docs. | Refresh state docs after each corrective commit. |
| 1. SDK 33 + cleanup | minSdk 33, Android 13+ docs, dead pre-33 cleanup. | Done: `minSdk = 33`, `targetSdk = 36`, `compileSdk 36.1`; ADR-006. | Unit/lint/debug build passed after README correction. | Mostly current after `f371643` and `756d944`. | No code task pending; keep device-coverage caveat. |
| 2-3. Result model + engines | Sealed result model, validation/unit engines, BigDecimal-safe dosing wrappers. | Partial: `CalcResult`, `ValidationEngine`, `UnitConversionEngine`, `FertilizerChemistryEngine`, recommendation and medication safety/search exist. `DosingResult` is still 2-case; no `DosingCalculationEngine` wrapper. | JVM tests cover result, validation, unit conversion, fertilizer, recommendation, medication safety/search. Latest generated JVM report: 138 tests, 0 fail/error/skip. | SPEC still marks T4/T6 incomplete. | Implement T4/T6 only if product flow needs it; current app still uses `MainViewModel` + `SeachemCalculations` directly. |
| 4-5. Compose UI migration | Compose screens, Material 3 theme, parity, a11y, UI tests. | Screens are Compose-hosted in Fragment/XML shell; History route added; AI orphan removed; theme tokens moved. `MainViewModel` LiveData remains. | JVM tests cover VM/history/dose logging/theme; instrumented tests exist but current API-36 Espresso path is blocked/not CI-gated. | Design docs are stale in several places: History shown orphan/missing, AI shown orphan, smoke counts stale. | Update design docs; run compatible instrumented tests when device exists; StateFlow migration remains larger implementation phase. |
| 6. Medication module | Evidence-grounded catalog, safety gates, beginner/expert flows. | Partial: Kotlin in-code catalog, safety engine, search engine, Compose screen. Non-Seachem entries remain SECONDARY/verify-label. | Medication safety/search JVM tests exist. | Research report and schema docs exist but some claims are pending/stale. | Verify task #14 product evidence; expand permutation tests/rule tables only after source data is upgraded. |
| 7. Fertilizer module | DIY chemistry engine, stock/target solvers, macro/micro separation, UI. | Partial: molar/ppm engine and Compose screen exist. No stock-solution or nutrient-target solver found. | Fertilizer chemistry JVM tests exist. | Research/schema docs mark solubility/CSM+B pending. | Implement T8 if needed; verify solubility/CSM+B before richer dosing claims. |
| 8. Catalog/search/indexing | Schema-valid catalog, no duplicate IDs, FW/SW separation, richer seed. | Partial: medication search and in-code catalog exist; no Room/JSON seed. | Duplicate/search/safety tests exist. | `PRODUCT_CATALOG_SCHEMA.md` describes current in-code schemas. | Decide whether catalog size justifies Room/JSON; keep SECONDARY evidence warnings. |
| 9. QA / docs / security / accessibility / performance | Drift check, doc truth, security guidance, a11y, performance, compliance. | In progress: README Tech Stack corrected; `DOCUMENTATION_TRUTH_AUDIT.md` added. Security and notices exist, but audit found overclaims. | Unit/lint/debug build passed; no fresh instrumented/security/performance run in this phase. | Audit created; many source docs still stale. | Immediate next task: apply high-severity doc corrections DOC-001..DOC-008. Then run security/a11y/perf checks. |
| 10. Release readiness | Release build, signing boundary, SBOM/notices, final clean tree. | Partial: release APK artifact exists for v2.0; signing is conditional; Apache-2.0 license and notices exist. | Current turn did not rerun `assembleRelease`; CI does not run release build. | Release docs stale/overstated; no SBOM artifact found. | Rerun release build after doc/code fixes; add SBOM or document absence; final tree must exclude unrelated local edits. |

## Next Executable Phase

`Phase 9: QA/docs truth remediation` is next. It is not owner-gated, does not need external credentials, and directly blocks release-readiness claims.

First batch:

1. Amend ADR-003 / ADR-008 / ADR-011 to align History v2 schema and append-only semantics.
2. Update `CURRENT_STATE.md`, `FINAL_QA_REPORT.md`, and `TESTING_STRATEGY.md` to remove stale counts, stale release artifact names, unsupported CI/UI-test claims, and unsupported secret-guard claim.
3. Update `SECURITY.md`, `web/README.md`, ADR-004, and Base_Template docs to distinguish current implementation from planned PWA/parity work.

## Handoff

**Completed work:** phase matrix built from current evidence.
**Active risks:** docs are the immediate blocker; implementation gaps remain T4/T6/T8/T9/T10/T11/T12/T13.
**Tests already passed this turn:** `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`.
**Verification:** staged `git diff --check` clean.
