# SPEC — Seachem-Dosing v2.0 (aquarium super-app)

distilled 2026-06-27 from code + DEEP_RESEARCH_REPORT.md + MODERNIZATION_PLAN.md + ADR-001..006. caveman format. `?` = unconfirmed.

## §G
precision-safe aquarium dosing + fertilizer + medication engines. evidence-grounded, safety-gated decision support. NOT veterinary diagnosis.

## §C
- minSdk 33, target/compile 36 [ADR-006]
- Compose + Koin + Room [ADR-001/002/003]; offline-first; no backend
- parity: `logic/Calculations.kt` ↔ `Base_Template/js/dosingCalculations.js` — coeffs identical
- no med/chem fact invented — manufacturer-sourced or UNKNOWN [DEEP_RESEARCH_REPORT]
- BigDecimal final math; round only at display
- no push/release/signing change w/o owner OK
- `web/` (TS) vs `Base_Template/` (JS) dual stack — owner decision pending (R6)

## §I
- in: `logic/SeachemCalculations` BigDecimal engine (Product, UnitScale, CalculationResult)
- in: `logic/Calculations` Double legacy (XML screens) — retire after Compose parity
- in: `domain/usecase/{LogAdministeredDoseUseCase,RecordWaterParameterReadingUseCase}`
- new: `core/result/CalcResult<T>` = Success<T> | NeedsMoreInput | UnsafeBlocked | Unsupported | CalculationError
- new: `domain/engine/{UnitConversion,Dosing,Fertilizer,Medication,Validation,SymptomTriage,InteractionSafety}`
- new: catalog seed JSON (Brand/Product/ActiveIngredient/DoseRule/Contraindication/EvidenceSource)

## §V
V1 | every user-facing dosing/med/fert calc returns sealed CalcResult. never throw to UI; never bare number.
V2 | final dosage math BigDecimal. rounding only at display boundary.
V3 | zero/neg/NaN/overflow/missing-conc/zero-vol → typed CalcResult (NeedsMoreInput | CalculationError). no crash, no silent 0.
V4 | FW/SW/reef compat never assumed. unknown ≠ compatible. FW med in SW (or reverse) → UnsafeBlocked unless label verifies both.
V5 | high-risk active (antibiotic/copper/formaldehyde/malachite-green) needs confirmed species+water-type+volume+inverts+filtration before dose. missing → NeedsMoreInput.
V6 | verified "remove carbon/UV/ozone + remove invertebrates" surfaced for every Seachem med [WS2].
V7 | macro/micro fertilizer mixes default separate (FePO4 precipitation) [WS3].
V8 | catalog records schema-valid; no duplicate product id.
V9 | unit conversion round-trips within rounding tolerance (L↔gal_US/UK, ppm↔dKH).
V10 | any dosing-coeff change = two-file patch (Kotlin + JS) + test; else parity broken [§C].

## §T
id|st|desc|cites
T1|x|core/result/CalcResult<T> sealed + fold/map helpers|V1,V3
T2|x|ValidationEngine: vol/conc/range → CalcResult|V3,V5
T3|x|UnitConversionEngine BigDecimal (L,gal_US/UK,ppm,dKH; meq/L pending)|V2,V9
T4|x|remove obsolete DosingResult staging; active result path is CalcResult|V1
T5|x|boundary tests: zero/neg/overflow/unit round-trip|V3,V9
T6|.|DosingCalculationEngine wrap SeachemCalculations under CalcResult|V1,V2
T7|x|FertilizerChemistryEngine molar/ppm BigDecimal|V2,V7
T8|.|StockSolutionSolver + NutrientTargetSolver|V2,V7
T9|.|catalog schema + entities + seed (12 meds, 8 fert compounds)|V6,V8
T10|.|MedicationDoseEngine + SymptomTriageEngine + InteractionSafetyEngine rule tables|V4,V5,V6
T11|.|med permutation tests (single/multi/contradictory/FW-SW/dup-class)|V4,V5
T12|.|Compose migration, parity-gated [ADR-001]|-
T13|.|catalog search/index + FW/SW default filter|V4,V8

## §B
id|date|cause|fix
