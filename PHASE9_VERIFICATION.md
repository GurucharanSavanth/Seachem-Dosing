# Phase 9 Verification — security, calculation parity, build

**Date:** 2026-06-30 · **Branch:** `v2.0-wip` · **Baseline:** `63b3110`
**Method:** inline read-only verification (the background multi-agent audit workflow
was abandoned after it failed to survive session restarts; the high-value checks —
calc parity, security, build — are done directly and are more reliable that way).

> Located at repo root because `docs/audit/` is currently owned by `root` (created
> by an earlier elevated-context session) and is not writable by the working user.

## 1. Calculation parity (the CLAUDE.md "sync invariant") — PASS

Kotlin (`app/.../logic/Calculations.kt`) vs JS (`Base_Template/js/utils.js`,
`dosingCalculations.js`). Every shared coefficient is numerically identical:

| Constant | Value (both sides) |
|---|---|
| US_GAL_TO_L / UK_GAL_TO_L | 3.78541 / 4.54609 |
| CM3_TO_L / IN3_TO_L / FT3_TO_L | 0.001 / 0.0163871 / 28.3168 |
| PPM_TO_DH | 17.86 |
| COEFF_KHCO3_STOICH | 0.0357 |
| COEFF_EQUILIBRIUM | 16/(80·3) ≈ 0.0667 |
| GPL_MIN_NR / GPL_MAX_NR | 0.0625 / 0.125 |
| COEFF_ACID | 1.5/(40·2.8) ≈ 0.01339 |
| COEFF_GOLD_FULL | 6/40 = 0.15 |
| COEFF_SAFE | 1/200 = 0.005 |
| COEFF_APT_80PCT | 0.8·(3/100) = 0.024 |
| APT_NITRATE_EST_PER_ML | 1.5 |
| PRIME_ML_PER_L / STABILITY_ML_PER_L | 5/200 / 5/40 |

All primary dose formulas (KHCO₃, Equilibrium, Neutral Regulator, Acid Buffer,
Gold Buffer, Safe, Prime, Stability, APT) produce identical output for valid input.
`SeachemCalculations.kt` / `SaltMixCalculations.kt` do **not** redefine any
coefficient (`rg COEFF_|GPL_|_ML_PER_L` → empty) — they consume `Calculations.*`,
so there is one source of truth on the Android side.

### Known low-severity divergences (invalid input only — not real-world reachable)

| ID | Where | Kotlin | JS | Real-world impact |
|---|---|---|---|---|
| CALC-1 (LOW) | `calculateKhco3Grams` | clamps `purity ≥ 0.01` (`MIN_PURITY`) | divides by raw `purity` (guards only `≤0`) | None: purity is documented 0.5–1.0; sub-1% purity cannot occur. |
| CALC-2 (LOW) | `calculateNeutralRegulatorGrams` | `safeKh = max(0, currentKh)` | uses raw `currentKh` | None: KH ≥ 0 always; Android ViewModel `coerce*` blocks negatives. |
| CALC-3 (INFO) | `calculateAptCompleteDose` | no dead var | JS computes an `estimatedNitrateIncrease` then discards it, returns `ml·0.015` | None: returned values match; JS has one dead local. |

CALC-1/2/3 are not dosing divergences for any input a user can produce. No fix
required for parity; if desired, mirror the two guards into the JS for symmetry
(2-line change each) — deferred as cosmetic.

## 2. Security (static, offline-app posture) — PASS

| Surface | Finding |
|---|---|
| Permissions | `AndroidManifest.xml` declares **zero** `uses-permission`. No INTERNET → no network exfiltration path exists. |
| Exported components | One `android:exported="true"` — `MainActivity`, the LAUNCHER activity (MAIN/LAUNCHER intent-filter). Required and correct. No exported services/receivers/providers. |
| Backup | `android:allowBackup="false"` — blocks `adb backup` data extraction. |
| Secrets | `git grep` for API keys / private keys / hardcoded passwords → none. The `storePassword`/`keyPassword` in `app/build.gradle.kts` are `provider.get()` reads of `SEACHEM_RELEASE_*` env/Gradle props, not literals. `google-services.json` is gitignored and untracked. |

No critical or high security finding. Matches `SECURITY.md` posture.

## 3. Build / release gate — PASS

- `assembleRelease` (R8 minify + resource shrink) re-run at baseline `63b3110`
  after the strings.xml URL fix: **BUILD SUCCESSFUL in 1m 56s** (exit 0).
  Output APK `SeachemDosing-v2.0-release.apk` (~16.3 MB) regenerated 2026-06-30.
- `processReleaseResources` re-ran (strings.xml changed) and succeeded — confirms
  the resource change compiles through the full release pipeline.
- Unit/lint/debug gate (138 tests, lintDebug, assembleDebug) was green at the
  parent commit per `CURRENT_STATE.md`; the only changes since are string/doc-only
  and cannot affect Kotlin compilation.

## 4. Not done (hard-blocked / out of scope)

- `connectedDebugAndroidTest` / Compose instrumented tests / emulator performance:
  `adb` is not on PATH — no device tooling available. Unchanged blocker.
- Web `package.json` major dep bumps (Vite 8, Vitest 4) sit uncommitted in the
  working tree, untested — left for a dedicated web-build validation task.

## Verdict

Phase 9 (static security + calculation integrity) and the Phase 10 release-build
gate are **GREEN**. Remaining open items are device-tooling-blocked or owner-gated;
none is a safety-critical or security-critical defect.
