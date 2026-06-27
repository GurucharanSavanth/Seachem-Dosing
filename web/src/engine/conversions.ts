import type { UnitScale, VolumeUnit } from './types.js';

// Mirror SeachemCalculations.kt private conversion factors verbatim.
// Slight numerical drift from 1/US_GAL_TO_L is intentional — the literal
// 0.264172 matches the Kotlin BigDecimal source exactly.
const MEQ_L_TO_DKH = 2.8;
const MEQ_L_TO_PPM = 50.0;
const L_TO_US_GAL = 0.264172;
const UK_GAL_TO_US_GAL = 1.20095;
const UK_GAL_TO_L_FACTOR = 4.54609;

const FORMAT_DECIMALS = 3;

/** Volume → US gallons. Mirrors SeachemCalculations.toUsGal. */
export function toUsGal(volume: number, unit: VolumeUnit): number {
  if (unit === 'US') return volume;
  if (unit === 'UK') return volume * UK_GAL_TO_US_GAL;
  return volume * L_TO_US_GAL;
}

/** Volume → litres. Mirrors SeachemCalculations.toLitres. */
export function toEngineLitres(volume: number, unit: VolumeUnit): number {
  if (unit === 'L') return volume;
  if (unit === 'US') return volume / L_TO_US_GAL;
  return volume * UK_GAL_TO_L_FACTOR;
}

/**
 * Convert a hardness/concentration value between scales.
 * Returns the value unchanged when fromScale === toScale, or when either
 * side is pH (no scale conversion meaningful).
 *
 * Mirrors SeachemCalculations.convertInput.
 */
export function convertInput(
  value: number,
  fromScale: UnitScale,
  toScale: UnitScale
): number {
  if (fromScale === toScale) return value;
  if (fromScale === 'pH' || toScale === 'pH') return value;

  let meqL: number;
  switch (fromScale) {
    case 'meq/L':
      meqL = value;
      break;
    case 'dKH':
      meqL = value / MEQ_L_TO_DKH;
      break;
    case 'ppm':
      meqL = value / MEQ_L_TO_PPM;
      break;
    default:
      return value;
  }

  switch (toScale) {
    case 'meq/L':
      return meqL;
    case 'dKH':
      return meqL * MEQ_L_TO_DKH;
    case 'ppm':
      return meqL * MEQ_L_TO_PPM;
    default:
      return meqL;
  }
}

/** Round to FORMAT_DECIMALS — mirrors SeachemCalculations.format(BigDecimal). */
export function format(value: number): number {
  const f = 10 ** FORMAT_DECIMALS;
  return Math.round(value * f) / f;
}
