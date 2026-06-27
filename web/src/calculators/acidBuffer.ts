import { COEFF_ACID } from '../core/constants.js';

/**
 * Seachem Acid Buffer dose in grams.
 *
 * Only used to *lower* KH (target < current); returns 0 otherwise.
 * Reference: 1.5 g per 40 L for a 2.8 dKH drop (per Seachem label).
 *
 * @see Calculations.kt#calculateAcidBufferGrams
 */
export function calculateAcidBufferGrams(
  litres: number,
  currentKh: number,
  targetKh: number
): number {
  const deltaKh = currentKh - targetKh;
  const grams = deltaKh * COEFF_ACID * litres;
  return Math.max(0, grams);
}
