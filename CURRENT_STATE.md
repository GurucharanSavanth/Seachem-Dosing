# CURRENT_STATE — autonomous execution handoff

**Branch:** v2.0-wip · **Last verified commit:** e140e7d (history schema correction, 130 JVM tests green)

## Phase
History v2 — Phase 1 (Commit E: Room v1→v2 migration) in progress.

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
