import {
  CM3_TO_L,
  FT3_TO_L,
  IN3_TO_L,
  PPM_TO_DH,
  UK_GAL_TO_L,
  US_GAL_TO_L
} from './constants.js';

export type VolumeUnit = 'L' | 'US' | 'UK';
export type DimensionUnit = 'cm' | 'in' | 'ft';
export type HardnessUnit = 'dh' | 'ppm';

/**
 * Volume → litres.
 * Mirrors Kotlin `Calculations.toLitres(volume, unit)`.
 */
export function toLitres(volume: number, unit: VolumeUnit): number {
  if (unit === 'US') return volume * US_GAL_TO_L;
  if (unit === 'UK') return volume * UK_GAL_TO_L;
  return volume;
}

/**
 * Litres → volume in the requested unit.
 * Mirrors Kotlin `Calculations.fromLitres(litres, unit)`.
 */
export function fromLitres(litres: number, unit: VolumeUnit): number {
  if (unit === 'US') return litres / US_GAL_TO_L;
  if (unit === 'UK') return litres / UK_GAL_TO_L;
  return litres;
}

/**
 * length × breadth × height + dimension unit → litres.
 * Returns 0 for non-positive or non-finite computed volume.
 */
export function dimensionsToLitres(
  length: number,
  breadth: number,
  height: number,
  unit: DimensionUnit
): number {
  const volume = length * breadth * height;
  if (volume <= 0 || !Number.isFinite(volume)) return 0;
  switch (unit) {
    case 'in':
      return volume * IN3_TO_L;
    case 'ft':
      return volume * FT3_TO_L;
    case 'cm':
    default:
      return volume * CM3_TO_L;
  }
}

/** ppm → degrees of hardness. Returns 0 for non-positive / non-finite input. */
export function ppmToDh(ppm: number): number {
  if (ppm <= 0 || !Number.isFinite(ppm)) return 0;
  return ppm / PPM_TO_DH;
}

/** dGH/dKH → ppm. Returns 0 for non-positive / non-finite input. */
export function dhToPpm(dh: number): number {
  if (dh <= 0 || !Number.isFinite(dh)) return 0;
  return dh * PPM_TO_DH;
}

export function convertHardness(
  value: number,
  from: HardnessUnit,
  to: HardnessUnit
): number {
  if (from === to) return value;
  if (from === 'ppm' && to === 'dh') return ppmToDh(value);
  if (from === 'dh' && to === 'ppm') return dhToPpm(value);
  return value;
}
