/**
 * Salt Mix Calculator.
 *
 * Mirrors:
 *   - Feature_Update/salt-mix-calculator.js (extracted from BRS calculator)
 *   - app/src/main/java/.../logic/SaltMixCalculations.kt
 *
 * Factor unit: grams per (US gallon × 1 PPT).
 */

export const SALT_MIX_PRODUCTS: Readonly<Record<string, number>> = Object.freeze({
  'Aquaforest Hybrid Pro Salt Mix': 4.3769,
  'Aquaforest Reef Salt Mix': 4.3769,
  'Aquaforest Reef Salt Plus Mix': 4.3769,
  'Aquaforest Sea Salt Mix': 4.3769,
  'Brightwell NeoMarine': 3.971428571,
  'HW-Marinemix Professional': 3.857142857,
  'HW-Marinemix Reefer': 3.857142857,
  'Instant Ocean Sea Salt Mix': 4.285714286,
  'Instant Ocean Reef Crystals': 4.285714286,
  'Nyos Pure Salt Mix': 4.339,
  'Red Sea Coral Pro': 4.114285714,
  'Red Sea Blue Bucket': 4.114285714,
  'Reef Crystals': 4.285714286,
  'Tropic Marin Bio-Actif': 4.2,
  'Tropic Marin Classic': 4.2,
  'Tropic Marin Pro Reef': 4.2,
  'Tropic Marin SynBiotic': 4.2
});

const GRAMS_TO_POUNDS = 0.00220462;

export interface SaltMixResult {
  productName: string;
  factor: number;
  grams: number;
  kilograms: number;
  pounds: number;
}

function roundTo(value: number, decimals: number): number {
  if (!Number.isFinite(value) || decimals < 0) return Number.NaN;
  const f = 10 ** decimals;
  return Math.round((value + Number.EPSILON) * f) / f;
}

/**
 * @param productName Must exist in SALT_MIX_PRODUCTS
 * @param volumeGallons US gallons, > 0
 * @param currentPpt   ≥ 0
 * @param desiredPpt   > currentPpt and ≤ 50 PPT
 * @returns null when any input is invalid (mirrors Kotlin behavior)
 */
export function calculateSaltMix(
  productName: string,
  volumeGallons: number,
  currentPpt: number,
  desiredPpt: number
): SaltMixResult | null {
  if (volumeGallons <= 0) return null;
  if (currentPpt < 0) return null;
  if (desiredPpt < 0) return null;
  if (desiredPpt <= currentPpt) return null;
  if (desiredPpt > 50) return null;

  const factor = SALT_MIX_PRODUCTS[productName];
  if (factor === undefined) return null;

  const deltaPpt = desiredPpt - currentPpt;
  const gramsRaw = deltaPpt * volumeGallons * factor;

  if (!Number.isFinite(gramsRaw) || gramsRaw < 0) return null;

  return {
    productName,
    factor,
    grams: roundTo(gramsRaw, 1),
    kilograms: roundTo(gramsRaw / 1000, 3),
    pounds: roundTo(gramsRaw * GRAMS_TO_POUNDS, 3)
  };
}
