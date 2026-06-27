import { COEFF_GOLD_FULL } from '../core/constants.js';
import type { GoldBufferResult } from '../core/types.js';

/**
 * Seachem Gold Buffer (goldfish-specific pH raiser) dose.
 *
 * Recommends full dose when delta ≥ 0.3 pH; else half dose.
 * Reference: 6 g per 40 L for full dose (per Seachem label).
 *
 * @see Calculations.kt#calculateGoldBufferGrams
 */
export function calculateGoldBufferGrams(
  litres: number,
  currentPh: number,
  targetPh: number
): GoldBufferResult {
  const deltaPh = targetPh - currentPh;
  if (deltaPh <= 0) return { grams: 0, fullDose: false };
  const fullDose = deltaPh >= 0.3;
  const doseMultiplier = fullDose ? 1 : 0.5;
  const grams = COEFF_GOLD_FULL * doseMultiplier * litres;
  return { grams: Math.max(0, grams), fullDose };
}
