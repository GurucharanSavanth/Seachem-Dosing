import { GPL_MAX_NR, GPL_MIN_NR } from '../core/constants.js';

/**
 * Seachem Neutral Regulator dose in grams.
 *
 * Adaptive: KH-modulated within [GPL_MIN_NR, GPL_MAX_NR] g/L per 0.5 pH step,
 * capped at GPL_MAX_NR × litres × 2 to prevent extreme overshoots.
 *
 * Returns 0 if target ≥ current (Neutral Regulator only lowers pH).
 *
 * @see Calculations.kt#calculateNeutralRegulatorGrams
 */
export function calculateNeutralRegulatorGrams(
  litres: number,
  currentPh: number,
  targetPh: number,
  currentKh: number
): number {
  if (targetPh >= currentPh) return 0;
  const safeKh = Math.max(0, currentKh); // mirrors Kotlin safeKh = max(0, currentKh)
  const khEffectFactor = Math.min(safeKh, 4) / 4;
  const baseGramsPerLitre =
    GPL_MIN_NR + (GPL_MAX_NR - GPL_MIN_NR) * khEffectFactor;
  const phSteps = (currentPh - targetPh) / 0.5;
  if (phSteps <= 0) return 0;
  const grams = Math.min(
    baseGramsPerLitre * litres * phSteps,
    GPL_MAX_NR * litres * 2
  );
  return Math.max(0, grams);
}
