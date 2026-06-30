# ADR-008: Build & connect the History feature

**Status:** Accepted
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Related:** ADR-003 (Room)

**Current implementation note (2026-06-29):** the decision to connect History is
implemented. The old v1 DAO/repository names and destructive delete/clear scope
below are historical; ADR-011 supersedes them with the v2 append-only event schema
and correction/void events.

## Context
Room persistence existed as an orphan vertical when this ADR was written. The current
implementation has `HistoryEventRepository`, `HistoryViewModel`, `HistoryScreen`,
dashboard "Save readings", calculator "Log as administered", and the History route
wired into the Fragment/XML shell.

## Decision
Complete the History vertical slice rather than delete built+tested infrastructure. Realize existing investment and the stated product goal.

## Scope
Navigation destination · Compose `HistoryScreen` · `HistoryViewModel` · immutable `HistoryUiState` · UI events · repository/use-case integration · states: loading / empty / populated / recoverable-error · filtering · sorting · search where justified · record-detail view · state restoration · accessibility semantics · responsive phone/tablet layout.

Explicitly out of current scope: physical delete-one / clear-all. History is
append-only; corrections and voids are represented by new events (ADR-011).

## Data-mapping discipline
Expose **only** fields the Room schema actually supports. Do not surface UI-designed fields with no backing column. Candidate fields *if represented by schema*: calculation type, tank/profile id, inputs+units, results+units, formula/rule id, evidence/source id, warnings, timestamp, app version, engine version, rounding policy, audit metadata. Verify each against `data/local/entity/*` before binding; mark unsupported as out-of-scope, not fabricated.

## Tests (required before "done")
DAO tests · repository tests · use-case tests · `HistoryViewModel` tests · Compose UI tests · navigation tests · Room migration test for **every** schema version · empty/loading/error/populated screenshot baselines · large-history performance test · invalid/corrupt-record handling test.

## Consequences
- **Easier:** history becomes a real feature; orphan layer justified; audit-trail surface for calculations.
- **Harder:** new destination in a Fragment shell (ADR-007) — host `HistoryScreen` via `ComposeView` like other screens; migration-test maintenance.

## Rollback
Feature is additive; revert the History commits if blocked. Do not remove the Room layer under this ADR (that was the rejected alternative).
