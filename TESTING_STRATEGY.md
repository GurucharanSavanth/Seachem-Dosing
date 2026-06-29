# TESTING_STRATEGY.md

**Project:** Seachem-Dosing v2.0 · **Branch:** `v2.0-wip` · **Updated:** 2026-06-29

## Layers

| Layer | Tool | What | Status |
|---|---|---|---|
| Unit (JVM) | JUnit4 | engines, result model, safety gates, chemistry, search, history | latest generated report: 138 tests, 0 failures/errors/skips |
| On-device smoke | adb (`installDebug` + `screencap` + `input tap`) | Compose screen rendering + key interactions | historical Pixel_10_Pro_XL (API 36) smoke; not rerun in docs-remediation workstream |
| Instrumented (Compose) | androidx.compose.ui.test | `ProfileSelectionScreenTest` | exists, but not CI-gated; prior API-36 emulator run blocked on Espresso `InputManager.getInstance` |
| Lint | `lintDebug` | unused resources, a11y, correctness | run each phase |
| Web parity | `scripts/verify-sync.js` (legacy) | `Calculations.kt` ↔ `Base_Template` constants only | does not cover `web/src/**` or `SeachemCalculations.kt` yet |

## Unit suites (`app/src/test`)

- `core/result/CalcResultTest` — 5-state sealed result, map/fold/failureOrNull (§V1).
- `domain/engine/UnitConversionEngineTest` — BigDecimal conversions + round-trips + negative→error (§V2/V3/V9).
- `domain/engine/ValidationEngineTest` — zero/negative/null/out-of-range → typed results (§V3).
- `domain/engine/FertilizerChemistryEngineTest` — KNO₃ 1 g/100 L → N 1.385 / K 3.867 ppm; round-trip; unknown→Unsupported; negative→error (§V2/V7).
- `domain/engine/RecommendationEngineTest` — FW/SW/pond threshold→message **parity lock** (§V — dashboard advice cannot silently drift).
- `domain/medication/MedicationSafetyEngineTest` — §V4 FW/SW block, §V5 high-risk→NeedsMoreInput + inverts/copper block, dup-active block, §V6 carbon-removal surfaced.
- `domain/medication/MedicationSearchEngineTest` — §V4 water-type separation, §V8 no duplicate IDs, ranking.
- Inherited/current support: `CalculationsTest`, `SeachemCalculationsTest`, `MainViewModelTest`, `di/KoinVerifyAllTest`, `HistoryWriteUseCasesTest`, History repository/DAO/migration tests.

## Historical on-device verification log (Pixel_10_Pro_XL)

| Screen | Verified |
|---|---|
| ProfileSelection | renders, tap-select moves accent border, POND→"Sand and Gravel", Continue→nav |
| Settings | all rows + current values, bottom-nav nav |
| Dashboard | volume calc 37.9 L (10 US gal), param status bars, GH/KH toggles, engine recommendations identical to XML, Copy/Share |
| Calculators | volume-based doses compute (Flourish 0.833 mL), expand → scale dropdown + labelled inputs |
| Medication | disclaimer, Beginner chips, Expert "Assess safety" → **§V5 gate fired** (high-risk needs context) |
| Fertilizer | 8 compounds grouped, Expert shows live engine fractions (KNO₃ 13.9% N / 38.7% K) |

## Command log

```
./gradlew clean assembleDebug testDebugUnitTest lintDebug --continue   # baseline
./gradlew assembleDebug testDebugUnitTest                              # per phase
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.seachem_dosing.ui.profile.ProfileSelectionScreenTest
adb install + am start + screencap + input tap                        # on-device smoke
```

## Coverage priorities (next)

1. Per-engine boundary expansion (overflow, NaN, extreme volumes) — partially covered, extend.
2. Medication permutation matrix (multi-symptom, contradictory, prior-failed) — beginner flow.
3. Compose UI tests on an **API ≤ 34 AVD** (unblocks the Espresso path).
4. Web parity once the `Base_Template` vs `web/` decision (R6) is made.
