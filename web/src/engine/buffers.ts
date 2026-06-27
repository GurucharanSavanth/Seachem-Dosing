import { convertInput, format, toEngineLitres, toUsGal } from './conversions.js';
import type { EngineResult, UnitScale, VolumeUnit } from './types.js';

/**
 * Alkaline Buffer (raises KH) — meq/L base.
 * Mirrors SeachemCalculations.calculateAlkalineBuffer.
 */
export function calculateAlkalineBuffer(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const grams = diff * 3.5 * (volGal / 10);
  const tspns = grams / 7;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tspns),
    secondaryUnit: 'tsp'
  };
}

/**
 * Neutral Regulator (lowers pH) — uses |current - desired|.
 * Mirrors SeachemCalculations.calculateNeutralRegulator.
 */
export function calculateNeutralRegulator(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit
): EngineResult {
  const liters = toEngineLitres(volume, volumeUnit);
  const diff = Math.abs(current - desired);
  const gramsPerLiterPer05 = 5 / 40;
  const steps = diff / 0.5;
  const grams = steps * gramsPerLiterPer05 * liters;
  const tspns = grams / 5;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tspns),
    secondaryUnit: 'tsp'
  };
}

/**
 * Acid Buffer (lowers KH) — dKH base.
 * Returns zero result when delta ≤ 0 (cannot raise KH with acid buffer).
 * Mirrors SeachemCalculations.calculateAcidBuffer.
 */
export function calculateAcidBuffer(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const liters = toEngineLitres(volume, volumeUnit);
  const curDkh = convertInput(current, scale, 'dKH');
  const desDkh = convertInput(desired, scale, 'dKH');
  const diff = curDkh - desDkh;
  if (diff <= 0) {
    return { primaryValue: 0, primaryUnit: 'g', secondaryValue: 0, secondaryUnit: 'tsp' };
  }
  const grams = diff * 0.01339 * liters;
  const tspns = grams / 8;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tspns),
    secondaryUnit: 'tsp'
  };
}

/**
 * Equilibrium (raises GH) — meq/L base, US-gallon scaled.
 * Mirrors SeachemCalculations.calculateEquilibrium.
 */
export function calculateEquilibrium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const grams = (volGal / 20) * (16 * diff);
  const tbsp = grams / 16;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tbsp),
    secondaryUnit: 'tbsp'
  };
}

/**
 * Potassium Bicarbonate (raises KH) — dKH base, purity assumed 1.0.
 * Mirrors SeachemCalculations.calculatePotassiumBicarbonate.
 */
export function calculatePotassiumBicarbonate(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const liters = toEngineLitres(volume, volumeUnit);
  const curDkh = convertInput(current, scale, 'dKH');
  const desDkh = convertInput(desired, scale, 'dKH');
  const diff = desDkh - curDkh;
  const purity = 1.0;
  const grams = (diff * 0.0357 * liters) / purity;
  const tsp = grams / 5;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tsp),
    secondaryUnit: 'tsp'
  };
}

/**
 * Reef Buffer — meq/L base, primary in tsp.
 * Mirrors SeachemCalculations.calculateReefBuffer.
 */
export function calculateReefBuffer(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const tspns = (diff / 0.5) * (volGal / 40);
  const grams = tspns * 5;
  return {
    primaryValue: format(tspns),
    primaryUnit: 'tsp',
    secondaryValue: format(grams),
    secondaryUnit: 'g'
  };
}

/**
 * Reef Builder — meq/L base.
 * Mirrors SeachemCalculations.calculateReefBuilder.
 */
export function calculateReefBuilder(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const grams = 0.32 * (volGal * diff);
  const tspns = grams / 6;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tspns),
    secondaryUnit: 'tsp'
  };
}

/**
 * Reef Carbonate — liquid, mL primary.
 * Mirrors SeachemCalculations.calculateReefCarbonate.
 */
export function calculateReefCarbonate(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const ml = diff * volGal;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/**
 * Reef Fusion 2 (alkalinity component) — meq/L base, mL primary.
 * Mirrors SeachemCalculations.calculateReefFusion2.
 */
export function calculateReefFusion2(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const curMeq = convertInput(current, scale, 'meq/L');
  const desMeq = convertInput(desired, scale, 'meq/L');
  const diff = desMeq - curMeq;
  const ml = (volGal / 6.5) * (diff / 0.176);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}
