# ADR-011: Precision-Safe Append-Only History Event Schema

**Status:** Accepted — **Supersedes ADR-008** (the "map only schema-supported fields / no migration" constraint of the History decision).
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Note on numbering:** the owner suggested "ADR-009" for this record; that number is already taken by [ADR-009 (colour tokens)](adr-009-colour-tokens.md), so it is recorded here as ADR-011 (ADR-010 = AI removal). ADR-008 is **not** edited retrospectively.

## Context
The History feature (ADR-008) was scoped to wire the *existing* Room v1 schema. The approved write-path requirements (explicit "Log as administered" + "Save reading", audit fields, precision-safe storage, idempotency, append-only corrections) need data the v1 schema cannot hold:
- v1 `dosing_log` = `product/amount(Double)/unit/volume_litres_at_dose(Double)/administered/notes/timestamp`; v1 `parameter_log` = 17 nullable `Double` params + `volume_litres(Double)/notes/timestamp`.
- v1 stores doses as binary `Double` — incompatible with the project precision invariant (engines use `BigDecimal`/`CalcResult`).
- v1 has no event-type, app/engine version, formula/evidence id, route, concentration, calculated-vs-administered split, rounding policy, source module, idempotency key, or creation timestamp.

This ADR therefore redesigns the history persistence layer and defines a non-destructive `Migration(1, 2)`.

## Decision

### 1. Append-only event model (3 tables, not one wide nullable table)
```
history_event
  event_id (PK)            event_type (enum)        aquarium_profile_id
  occurred_at              created_at               source_module
  app_version              engine_version (nullable) idempotency_key (unique)
  schema_version           precision_status (enum)  notes (nullable)
  correction_of_event_id (nullable, FK self)        voided (bool)  void_reason (nullable)

dose_event_detail (FK event_id -> history_event)
  event_id (PK/FK)         product_id               product_variant_id (nullable)
  formula_rule_id (null)   evidence_source_id (null) route (enum, nullable)
  concentration_decimal (null)  concentration_unit (null)
  tank_volume_decimal      tank_volume_unit
  calculated_amount_decimal (null) calculated_amount_unit (null)
  administered_amount_decimal      administered_amount_unit
  rounding_mode (null)     rounding_scale (null)    user_modified_amount (bool)
  warnings_acknowledged (text, nullable)

parameter_event_detail (FK event_id -> history_event)
  event_id (PK/FK)         parameter_type (enum)    measured_value_decimal
  measured_unit            test_method (null)       source_device_or_kit (null)
  validation_status (enum)
```
- One applicable detail row per event, enforced by repository validation + DAO transaction + FK. `dose`/`parameter` detail tables only; **no** medication/fertilizer/maintenance/livestock detail tables until their requirements are defined.
- FK enforcement enabled and verified (`PRAGMA foreign_keys=ON` via Room's `setForeignKeyConstraintsEnabled` / `@Database` builder).

### 2. Precision-safe decimal persistence
- No `REAL`/`Float`/`Double` for any dose, concentration, volume, or measured value.
- Persist via `StoredDecimal(val canonicalValue: String)` where `canonicalValue == BigDecimal.toPlainString()`, reconstructed with the strict `BigDecimal(String)` ctor. Room `TypeConverter` is **lossless `BigDecimal ↔ canonical String`, no rounding**, covered by round-trip + malformed-input tests, representation documented here.
- Rules: no scientific notation, no locale separators, no grouping, no implicit binary conversion, no silent scale reduction, reject malformed/NaN/Infinity before persistence, defined max precision/scale/length, explicit rounding only at domain/display boundaries.
- Columns named `*_decimal` (`calculated_amount_decimal`, `administered_amount_decimal`, `tank_volume_decimal`, `concentration_decimal`, `measured_value_decimal`). No bare `amount` text column.

### 3. Unit identity (independent, coded)
Every numeric quantity carries an explicit **unit code** (stable enum, not a display label), inferred from nothing: `MILLILITER, LITER, US_GALLON, GRAM, MILLIGRAM, PPM_MG_PER_L, TEASPOON_US, TEASPOON_METRIC, MANUFACTURER_SCOOP, …`. A `MANUFACTURER_SCOOP` requires a verified product-specific scoop definition; a teaspoon record specifies its standard — no silent conflation of US/metric/calibrated spoon volumes.

### 4. Registries (enums, versioned here)
- **EventType:** `DOSE_ADMINISTERED, WATER_PARAMETER_READING_RECORDED, LEGACY_DOSE_RECORD, LEGACY_PARAMETER_RECORD, DOSE_CORRECTION, DOSE_VOID` (calculation-only events like `DoseCalculationGenerated` are **not** persisted in the initial timeline).
- **PrecisionStatus:** `NEW_EXACT_RECORD, LEGACY_BINARY64_APPROXIMATION, UNKNOWN_PRECISION`.
- **Route / ValidationStatus:** defined alongside.

### 5. Legacy `Migration(1, 2)` semantics (non-destructive)
Explicit `Migration(1,2)`; **no** `fallbackToDestructiveMigration`. For each v1 row:
1. create a `history_event`;
2. classify honestly — `LEGACY_DOSE_RECORD` / `LEGACY_PARAMETER_RECORD` (**not** `DOSE_ADMINISTERED` unless v1 proves administration; v1 `administered` boolean MAY refine this — `administered=true` → still `LEGACY_DOSE_RECORD` with a flag, since v1 cannot prove the audit context);
3. convert `Double` via `BigDecimal.valueOf(d).toPlainString()` (preserves the canonical decimal of the stored binary64; does **not** recover lost user precision);
4. `precision_status = LEGACY_BINARY64_APPROXIMATION`;
5. unavailable audit fields → `NULL`/`UNKNOWN`/explicit legacy enum — **never fabricate** formula/evidence ids, engine versions, routes, concentrations, rounding policies, products, warnings, or command idempotency keys;
6. preserve original timestamps/notes/product/units/values;
7. deterministic legacy ids `legacy-v1:<table>:<pk>` to prevent duplicate import;
8. run in a transaction;
9. retain v1 tables until migrated row counts are verified inside the migration;
10. drop/rename old tables only after successful copy + validation.

### 6. Audit fields & nullability by precision class
Separate validation per `NEW_EXACT_RECORD` vs `LEGACY_BINARY64_APPROXIMATION` vs `UNKNOWN_PRECISION`. A **new** dose-administration record requires: event id, event type, profile id, occurred_at, created_at, source_module, idempotency_key, administered_amount_decimal, administered_unit, tank_volume_decimal, tank_volume_unit, precision_status, app_version, engine_version (when an engine produced the value). Formula/evidence ids required **only** when the record originated from a sourced rule — no placeholder source ids.

### 7. Append-only corrections
Safety-relevant records are append-only. No silent edit of an administered-dose record. Corrections via: append `DOSE_CORRECTION` referencing `correction_of_event_id`, or `DOSE_VOID` with reason. Original calculated/administered amounts are never overwritten. If plain deletion stays available, document that it reduces audit completeness.

### 8. Idempotency
Unique index on `idempotency_key` for new write commands. Legacy rows use deterministic keys (`legacy-v1:…`). The repository maps unique-key conflicts to an idempotent-success / duplicate-command result — never an unhandled DB exception.

### 9. Constraints & indexes (query-driven)
PKs, FKs, unique `idempotency_key`, indexes on `occurred_at`, `aquarium_profile_id`, `event_type`, and dose `product_id` — each justified by a History query. FK enforcement verified. No index without a query (mapping recorded here when queries are finalized).

## Test matrix (gates 3–4, before any UI)
- **Migration:** empty→v2; 1 row; many rows; max/min/small-fraction/large legacy values; negative/malformed if possible; null optionals; timestamp/note/unit preservation; row-count parity; deterministic ids; duplicate-migration prevention; FK integrity; transaction rollback on failure.
- **Decimal:** `0`; trailing zeros; very small/large; max precision/scale; negative-reject where prohibited; malformed-string reject; locale independence; canonical round-trip; no scientific notation; no rounding on persist; documented value-vs-scale equality policy.
- **Integration (with UI, later):** log dose; save reading; observe immediately; filter by type/profile; process recreation; repeated submit → one event; persistence failure ≠ success; legacy records show an approximate-precision indicator.

## Gates before History UI
1. ADR-011 written ✅ (this file).
2. v2 schema reviewed.
3. `Migration(1,2)` passes.
4. Decimal round-trip tests pass.
5. v1 data preserved with **no** fabricated audit metadata.
6. DB reads both migrated legacy + new exact records.

## Artifacts to commit
Room v1 + v2 exported schemas · `Migration(1,2)` impl · migration test fixtures · ADR-011 · ERD · schema data dictionary · decimal-storage spec · unit-code registry · event-type registry · precision-status registry.

## Commit sequence (owner-mandated)
1. History read-side screen + tests *(after schema gates pass)*; 2. dose-administration write trigger + tests; 3. water-parameter write use case + trigger; 4. integration/migration/regression tests. No unrelated changes per commit. `room-testing` to be added to `androidTestImplementation` for `MigrationTestHelper`.

## Consequences
- **Easier:** audit-grade, precision-faithful, append-only history aligned with the engines' `BigDecimal` invariant; honest legacy handling.
- **Harder:** 3-table model + converters + migration + ~25 tests before UI; larger surface than ADR-008 envisaged.

## Rollback
Pre-release (versionCode 1, no shipped data) → migration risk is low. If v2 proves unstable, revert the schema commits; v1 remains intact (non-destructive migration never drops v1 data before validation).
