# CURRENT_STATE — autonomous execution handoff

**Branch:** v2.0-wip · **Last verified commit:** e140e7d (history schema correction, 130 JVM tests green)

## Phase
History v2 — Phases 1–5 COMPLETE (migration + write triggers + screen + integration, device-verified).

- `15f1e64` migrate to v2 · `d2bf91c` migration+DAO instrumented (12/12)
- `75cb266` write-trigger use cases · `4dea031` History screen (VM+Compose+nav+top-bar action)
- `920e8c7` Save-readings dashboard trigger · `6d83014` end-to-end integration (16/16 instrumented)
- Gates: JVM 137/0/0, instrumented 16/0, lint clean, Koin verify ok, tree clean.

## Remaining for History
- "Log as administered" UI button on the calculator result (use case + integration-test done; UI button pending).
  Needs a unit-handling decision: calculator dose results in `g`/`mL` log cleanly; `tsp`/`caps`/`tbsp` are
  product-mass spoons with no verified per-product measure definition (same issue as legacy migration).

## Device-test blocker (recorded, non-fatal)
- **Compose UI tests** (`createComposeRule`/Espresso) FAIL on `emulator-5554` (Pixel_10_Pro_XL, API-"17" preview):
  `NoSuchMethodException: android.hardware.input.InputManager.getInstance` from `Espresso.onIdle` (espresso 3.6.1
  doesn't support this preview image). Migration/DAO/integration instrumented tests are unaffected (no Espresso).
  HistoryScreen states covered by `HistoryViewModelTest` + integration tests. Revisit with a stable API 34/35 AVD
  or espresso 3.7 before adding Compose UI tests.

## Completed gates (this session)
- `1d0744f` menuAnchor fix · `040f74c` UI audit + ADRs 007–010 · `8f88463` AI/chat removal
- `5dfd806` colour-token parity · `fa0b50a` ADR-011 · `d6605d7`+`3fedd31` StoredDecimal (precision)
- `6f38b59` registries · `d3c2a24` v2 entities · `ec408f4` DAO+repo · `e140e7d` legacy semantics

## Active decisions (authoritative = ADRs)
- ADR-007 retain Fragment shell · ADR-008 build History · ADR-009 Compose-first colour · ADR-010 remove AI
- ADR-011 (+§11): precision-safe append-only 3-table history; StoredDecimal canonical String; v1 writer
  orphaned ⇒ proportionate non-destructive migration; legacy tsp/tbsp = mass measures → LEGACY_UNSPECIFIED/
  engine-measure (never volume spoons); decimals = TEXT, repository is StoredDecimal boundary.

## Next executable task
Commit E: wire 3 v2 entities into AppDatabase v2 + Migration(1,2) + export 2.json + room-testing androidTest;
then instrumented migration/DAO tests on emulator-5554; then write triggers (Log administered / Save reading);
then History UI; then integration; then remaining master-prompt phases.

## Unresolved / deferred
- Orphan use cases (Calculate/Convert/Validate/QuickDose) + CalculationsRepository — dead from UI; cleanup in a later phase.
- Parameter↔unit dimensional validation for NEW readings (beyond structural) — later.
- R2 done (param units added). minSdk locked at 33 (ADR-006).
