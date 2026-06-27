import { COEFF_EQUILIBRIUM } from '../core/constants.js';

/**
 * GH Booster (Seachem Equilibrium) dose in grams.
 * Only raises GH; if delta ≤ 0, returns 0.
 *
 * Reference: 16 g per 80 L raises GH by 3 dGH (per Seachem label).
 *
 * @see Calculations.kt#calculateEquilibriumGrams
 */
export function calculateEquilibriumGrams(
  deltaGh: number,
  litres: number
): number {
  const grams = deltaGh * COEFF_EQUILIBRIUM * litres;
  return Math.max(0, grams);
}
