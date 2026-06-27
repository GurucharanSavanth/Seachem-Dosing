import { PRIME_ML_PER_L } from '../core/constants.js';

/**
 * Seachem Prime dose in mL.
 *
 * Used for ammonia/nitrite emergency detoxification. Reference:
 * 5 mL per 200 L (per Seachem label).
 *
 * @see Calculations.kt#calculatePrimeDose
 */
export function calculatePrimeDose(litres: number): number {
  return litres * PRIME_ML_PER_L;
}
