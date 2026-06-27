import { COEFF_SAFE } from '../core/constants.js';

/**
 * Seachem Safe dose in grams.
 *
 * Reference: 1 g per 200 L removes up to 4 ppm chlorine/chloramine
 * (per Seachem label).
 *
 * @see Calculations.kt#calculateSafeGrams
 */
export function calculateSafeGrams(litres: number): number {
  const grams = litres * COEFF_SAFE;
  return Math.max(0, grams);
}
