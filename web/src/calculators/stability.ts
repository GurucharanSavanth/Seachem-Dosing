import { STABILITY_ML_PER_L } from '../core/constants.js';

/**
 * Seachem Stability dose in mL.
 *
 * Used to seed/boost biological filtration. Reference: 5 mL per 40 L
 * (per Seachem label).
 *
 * @see Calculations.kt#calculateStabilityDose
 */
export function calculateStabilityDose(litres: number): number {
  return litres * STABILITY_ML_PER_L;
}
