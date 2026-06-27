/**
 * Cross-platform calculation constants.
 *
 * Mirror of:
 *   app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt
 *   Base_Template/js/utils.js
 *
 * Sync invariant (CLAUDE.md): every constant here must match its Kotlin
 * counterpart within 1e-9. Verified by `scripts/verify-sync.js` in CI.
 *
 * v1.0 source: Base_Template/js/utils.js v5.0
 */

// ===== Volume conversions =====
export const US_GAL_TO_L = 3.78541;
export const UK_GAL_TO_L = 4.54609;

// Dimension volume conversions
export const CM3_TO_L = 0.001;
export const IN3_TO_L = 0.0163871;
export const FT3_TO_L = 28.3168;

// ===== Hardness =====
export const PPM_TO_DH = 17.86;

// ===== Calculator coefficients =====

// KHCO3 stoichiometric — 0.357 mmol/L × 100.115 mg/mmol = 35.74 mg/L per dKH
export const KHCO3_MOLECULAR_WEIGHT = 100.115;
export const COEFF_KHCO3_STOICH = 0.0357;

// Seachem Equilibrium — official: 16g per 80L raises GH by 3 dGH
export const COEFF_EQUILIBRIUM = 16 / (80 * 3);

// Seachem Neutral Regulator — adaptive band, KH-modulated
export const GPL_MIN_NR = 0.0625;
export const GPL_MAX_NR = 0.125;

// Seachem Acid Buffer — official: 1.5g per 40L for 2.8 dKH drop
export const COEFF_ACID = 1.5 / (40 * 2.8);

// Seachem Gold Buffer — official: 6g per 40L for full dose
export const COEFF_GOLD_FULL = 6 / 40;

// Seachem Safe — official: 1g per 200L for chlorine/chloramine removal
export const COEFF_SAFE = 1 / 200;

// APT Complete (2Hr Aquarist) — 80% of standard 3ml/100L daily dose
export const COEFF_APT_STANDARD = 3 / 100;
export const COEFF_APT_80PCT = 0.8 * (3 / 100);
export const APT_NITRATE_EST_PER_ML = 1.5;

// Emergency dosing
export const PRIME_ML_PER_L = 5 / 200;
export const STABILITY_ML_PER_L = 5 / 40;
