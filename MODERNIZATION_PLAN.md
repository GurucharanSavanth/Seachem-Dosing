# MODERNIZATION_PLAN.md

**Project:** Seachem-Dosing → aquarium super-app v2.0
**Branch:** `v2.0-wip` (checkpoint `3b14d05`; `master` untouched at `f6df401`)
**Date:** 2026-06-27 · **Companion docs:** `DEEP_RESEARCH_REPORT.md`, `docs/architecture/adr-001..005.md`, `TOOL_UTILIZATION_LEDGER.md`

> Plan principle: **extend the in-progress v2.0, don't rebuild it.** The inherited tree already builds green and contains the clean-architecture skeleton, BigDecimal precision in the domain layer, Koin DI, Room, Compose wiring, and ADRs. This plan reconciles + finishes + extends, and cites evidence rather than re-deriving decisions.

---

## 1. Executive technical summary

- **Current state:** v2.0 modernization ~30-40% done, was uncommitted (now preserved on `v2.0-wip`). Builds green: `assembleDebug` OK, **13/13 unit tests pass**, lint 0 errors / 228 warnings. Toolchain already modern: AGP 8.13.2, Kotlin 2.0.21, Gradle 8.13, JDK 17, compileSdk 36.1 / targetSdk 36 / **minSdk 24** (→ bumping to 33).
- **Target:** aquarium super-app — reliable dosing calculators (existing), + medication decision-support module, + DIY fertilizer chemistry module, + product catalog/search, on a Compose/Material 3 UI (ADR-001), with precision-safe, evidence-grounded engines.
- **Resolved gates:** tree = checkpoint-then-continue; **minSdk = 33** (owner choice, ~68.9% device coverage — WS1); scope = full prescribed ceremony.
- **Top risks:** (R1) medication safety / no-hallucination; (R2) calc precision + unit safety; (R3) Compose migration regressions vs XML; (R4) FW/SW catalog mix-ups; (R5) web-research gaps (5 non-Seachem doses pending, F-06); (R6) dual web stack (`Base_Template/` vs `web/`).

## 2. Baseline audit (verified, Phase 0)

| Area | Status |
|---|---|
| Build | `./gradlew clean assembleDebug testDebugUnitTest lintDebug` → **SUCCESSFUL (1m26s)** |
| Tests | 5 suites, **13 tests, 0 fail/skip/error** |
| Lint | 0 errors, **228 warnings** (dual XML+Compose churn — triage P9) |
| Toolchain | AGP 8.13.2 / Kotlin 2.0.21 / Gradle 8.13 / JDK 17 |
| SDK | compile 36.1 / target 36 / **min 24** |
| EOL | repo-wide CRLF/LF flip — **fixed** via `.gitattributes` (eol=lf) |
| Secret | `app/google-services.json` (Firebase key) — **gitignored**, never committed |
| Junk | `.gradle_user/*.lck/.part` tracked at HEAD — `git rm --cached` in P1 |

## 3. Research ledger → see `DEEP_RESEARCH_REPORT.md`

WS1 Android (OFFICIAL), WS2 medication (7 Seachem OFFICIAL + 5 SECONDARY-pending), WS3 fertilizer chemistry (STANDARD, computed). Pending web items tracked as task #14.

## 4. Migration strategy (toolchain)

- Most toolchain modernization is **already done** (Compose/Koin/Room wired, JDK 17, SDK 36).
- **Phase 1 deltas:** minSdk 24→33; remove dead `SDK_INT<33` guards; `git rm --cached .gradle_user`; ADR-006 (minSdk). Verify green build after.
- No Gradle/AGP/Kotlin bumps needed (current + compatible).

## 5. Architecture refactor (reconcile, don't rebuild)

Inherited (keep): `domain/{model,usecase}`, `data/{local,repository}`, `di/` (Koin), `SeachemDosingApp`. Already BigDecimal at the domain boundary (`DosingResult.Success: BigDecimal`; `CalculateDoseUseCase` BigDecimal + zero-volume guard).

To add/extend:
- **Result model:** extend the 2-case `DosingResult` (Success/Error) to the safety-aware set: `Success | NeedsMoreInput | UnsafeBlocked | Unsupported | CalculationError` (generic `CalcResult<T>` reused by all engines).
- **Engines (domain/engine/):** `UnitConversionEngine`, `DosingCalculationEngine` (wrap existing Seachem math), `FertilizerChemistryEngine` + `StockSolutionSolver` + `NutrientTargetSolver`, `MedicationDoseEngine` + `SymptomTriageEngine` + `MedicationInteractionSafetyEngine`, `ValidationEngine`, `AuditLogEngine`.
- **Retire** legacy `logic/Calculations.kt` (Double) only as Compose screens replace the XML fragments that use it.
- `core/` for units + numeric precision + result types (new).

## 6. UI/UX revamp (per ADR-001, Compose)

- Build `ui/theme/` (Color/Theme/Type from existing `colors.xml`/`themes.xml`), reusable card composables (ADR-001 says 12 `card_*.xml` → ~3 composables).
- **Edge-to-edge is mandatory at targetSdk 36 (WS1)** — use Material 3 `Scaffold` / `WindowInsets`.
- Migrate ProfileSelection → Dashboard → Calculators → Settings (smallest first). Delete each `card_*.xml`/fragment **only after** Compose + nav + string + a11y parity + tests pass.
- New screens: medication beginner/expert flows, fertilizer flows, catalog/search.

## 7. Medication module (Phase 6) — evidence-grounded decision support, NOT diagnosis

- **Catalog** (Room + versioned JSON seed): Brand, Product, ActiveIngredient, TreatmentCategory, WaterTypeCompatibility, DoseRule, RouteOfAdministration, Contraindication, RequiredInput, EvidenceSource, EvidenceConfidence. Seed = the 12 researched products (Seachem OFFICIAL; others SECONDARY until task #14).
- **Engines as rule tables, not text generators:** `SymptomTriageEngine` (symptom→candidate, allowed/excluded water types, required confirmations, confidence, fail-safe), `MedicationInteractionSafetyEngine` (duplicate-active-ingredient, copper+invert, FW/SW mismatch).
- **Hard safety gates (sourced from WS2):** missing volume/species/water-type/route → `NeedsMoreInput`; invert present + Cu/most meds → `UnsafeBlocked`; reef + non-reef-safe → `UnsafeBlocked`; "remove carbon/UV/ozone" surfaced for every Seachem med (verified). Low confidence → "test water / quarantine / consult expert," never a dose.
- Beginner guided flow + expert calculator (exact dose only when all required evidence-backed inputs present).

## 8. DIY fertilizer module (Phase 7)

- `FertilizerChemistryEngine` using WS3 molar masses/fractions (BigDecimal). `ppm = dose_g × fraction × 1000 / volume_L`.
- Chemical catalog (formula, molar mass, nutrient fraction, FW/reef applicability, macro/micro, solubility[pending]). **Default macro/micro separation** (FePO₄ precipitation basis).
- Beginner chip flow + expert calculator; block precise dosing when concentration/volume/target missing.

## 9. Calculation integrity

- BigDecimal for all final dosage math (already at domain boundary); explicit rounding **only at display**.
- Dimensional units (L, gal_US/UK, mL, g, mg, ppm/mg·L⁻¹, mol/L, dKH, meq/L). Unit-safe conversions.
- Reject/confirm: negative/zero/extreme volume, missing concentration/route, unit mismatch, suspected overdose, unsupported species, conflicting prior med, carbon/UV-removal not acknowledged.
- Sealed `CalcResult` everywhere user-facing.

## 10. Testing strategy → `TESTING_STRATEGY.md` (P9)

Unit (JUnit/MockK), property-ish boundary tests (zero/neg/overflow/NaN/round-trip units), golden tests for known doses, Koin `verifyAll` (exists), Compose UI tests, medication permutation tests (single/multi/contradictory/high-risk/FW-SW-mismatch/duplicate-class), catalog schema + duplicate-ID tests, web parity (`scripts/verify-sync.js`).

## 11. Execution phases

| Phase | Scope | Gate |
|---|---|---|
| 0 ✅ | Checkpoint + baseline | green build |
| Step0/1 ✅ | Ledger + deep research | report delivered |
| **1** | minSdk 33 + cleanup + ADR-006 | green build + tests |
| 2-3 | Result model + engines (BigDecimal) + tests | tests green, parity kept |
| 4-5 | Compose design system + screen migration | parity + a11y + tests per screen |
| 6 | Medication catalog + engines + flows | safety-gate tests pass |
| 7 | Fertilizer engine + flows | chemistry tests pass |
| 8 | Catalog search/indexing + FW/SW separation | schema + dup-ID tests |
| 9 | QA: /check, code-review, security-guidance; docs | 0 critical findings |
| 10 | Final verify + caveman-stats + caveman-commit | green; no push |

## 12. File-level task list (initial — extended per phase)

| File | Action | Reason | Risk |
|---|---|---|---|
| `app/build.gradle.kts` | edit | minSdk 24→33 | low |
| `.gradle_user/**` | `git rm --cached` + ignore | tracked build cache | low |
| `domain/model/DosingResult.kt` | extend | add NeedsMoreInput/UnsafeBlocked/Unsupported/CalculationError | med (callers) |
| `logic/Calculations.kt` (Double) | retire-after-parity | superseded by BigDecimal path | med |
| `logic/SeachemCalculations.kt` | keep/verify | BigDecimal engine — confirm scale/rounding | med |
| `domain/engine/*` | create | new precision-safe engines | med |
| `data/local` catalog entities + seed | create | medication/fertilizer/product catalogs | med |
| `ui/theme/*`, `ui/<screen>/*Screen.kt` | create | Compose migration (ADR-001) | high |
| `res/layout/card_*.xml`, `ui/.../*Fragment.kt` | delete-after-parity | replaced by composables | high |
| `Base_Template/` vs `web/` | decide | dual web stack — owner call (R6) | med |

## 13. Acceptance criteria

Build + tests + lint(0 error) green · no unsupported medication/fertilizer claims (all graded; UNKNOWN never auto-filled) · no unit mismatch · no unsafe dose path (sealed-result gates) · no Compose/XML orphan state · no duplicate product IDs · no FW/SW catalog mix-up · no schema-invalid catalog records · `master` unchanged; no push/release without owner authorization.

## Open decisions for owner

- **R6 — dual web stack:** `Base_Template/` (vanilla JS, committed) vs `web/` (TS+Vite rewrite, was uncommitted). Keep both, retire `Base_Template/`, or retire `web/`? (Not blocking Android phases.)
- **CLAUDE.md is stale + gitignored** — update it to v2.0 reality? (It's gitignored, so I won't auto-edit.)
