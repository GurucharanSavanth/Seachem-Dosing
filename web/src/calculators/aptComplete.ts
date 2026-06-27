import { COEFF_APT_80PCT } from '../core/constants.js';
import type { AptResult } from '../core/types.js';

/**
 * APT Complete (2Hr Aquarist) dose in mL — 80 % of standard 3 ml/100 L
 * daily dose.
 *
 * Estimates the nitrate increase resulting from this dose so the UI can
 * surface a projected post-dose NO₃ figure to the user.
 *
 * @see Calculations.kt#calculateAptCompleteDose
 */
export function calculateAptCompleteDose(
  litres: number,
  currentNitrate = 0
): AptResult {
  const ml = litres * COEFF_APT_80PCT;
  const perLiterContribution = ml * 0.015;
  const estimatedFinalNitrate = currentNitrate + perLiterContribution;
  return {
    ml: Math.max(0, ml),
    estimatedNitrateIncrease: Math.max(0, perLiterContribution),
    estimatedFinalNitrate: Math.max(0, estimatedFinalNitrate)
  };
}
