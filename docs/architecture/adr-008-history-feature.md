# ADR-008: Build & connect the History feature

**Status:** Accepted
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Related:** ADR-003 (Room), `docs/design/UI_DOMAIN_CONNECTION_MATRIX.md` (ISSUE-CONN-003)

## Context
Room (`AppDatabase`, `DosingLogDao`, `ParameterLogDao`), `HistoryRepository(Impl)`, and use-cases exist and are tested, but **no UI consumes them** (grep-confirmed zero `ui/` references). The persistence layer is an orphan vertical with KSP/schema cost and no user value. Product goals list dosing/test history.

## Decision
Complete the History vertical slice rather than delete built+tested infrastructure. Realize existing investment and the stated product goal.

## Scope
Navigation destination · Compose `HistoryScreen` · `HistoryViewModel` · immutable `HistoryUiState` · UI events · repository/use-case integration · states: loading / empty / populated / recoverable-error · destructive-action confirmation (delete-one, clear-all) · filtering · sorting · search where justified · record-detail view · state restoration · accessibility semantics · responsive phone/tablet layout.

## Data-mapping discipline
Expose **only** fields the Room schema actually supports. Do not surface UI-designed fields with no backing column. Candidate fields *if represented by schema*: calculation type, tank/profile id, inputs+units, results+units, formula/rule id, evidence/source id, warnings, timestamp, app version, engine version, rounding policy, audit metadata. Verify each against `data/local/entity/*` before binding; mark unsupported as out-of-scope, not fabricated.

## Tests (required before "done")
DAO tests · repository tests · use-case tests · `HistoryViewModel` tests · Compose UI tests · navigation tests · Room migration test for **every** schema version · empty/loading/error/populated screenshot baselines · large-history performance test · invalid/corrupt-record handling test.

## Consequences
- **Easier:** history becomes a real feature; orphan layer justified; audit-trail surface for calculations.
- **Harder:** new destination in a Fragment shell (ADR-007) — host `HistoryScreen` via `ComposeView` like other screens; migration-test maintenance.

## Rollback
Feature is additive; revert the History commits if blocked. Do not remove the Room layer under this ADR (that was the rejected alternative).
