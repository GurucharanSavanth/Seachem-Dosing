import { COEFF_KHCO3_STOICH } from '../core/constants.js';

/**
 * KH Booster (Potassium Bicarbonate / KHCO₃) dose in grams.
 *
 * Stoichiometric: KHCO₃ MW = 100.115 g/mol; 1 dKH ≈ 0.357 meq/L.
 * Coefficient = 0.0357 g/L per dKH.
 *
 * Returns 0 for non-positive or non-finite purity, and clamps negative
 * deltas (target ≤ current) to 0 via the final Math.max.
 *
 * @see Calculations.kt#calculateKhco3Grams
 */
export function calculateKhco3Grams(
  currentKh: number,
  targetKh: number,
  litres: number,
  purity: number
): number {
  if (!Number.isFinite(purity) || purity <= 0) return 0;
  const safePurity = Math.max(0.01, purity); // mirrors Kotlin MIN_PURITY=0.01 clamp
  const grams = ((targetKh - currentKh) * COEFF_KHCO3_STOICH * litres) / safePurity;
  return Math.max(0, grams);
}
