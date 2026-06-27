import { convertInput, format, toUsGal } from './conversions.js';
import type { EngineResult, UnitScale, VolumeUnit } from './types.js';

function diffPpm(current: number, desired: number, scale: UnitScale): number {
  const curPpm = convertInput(current, scale, 'ppm');
  const desPpm = convertInput(desired, scale, 'ppm');
  return desPpm - curPpm;
}

/**
 * Reef Advantage Calcium — dry, primary tsp.
 * Mirrors SeachemCalculations.calculateReefAdvantageCalcium.
 */
export function calculateReefAdvantageCalcium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const tspns = 0.0019 * volGal * diff;
  const grams = tspns * 5;
  return {
    primaryValue: format(tspns),
    primaryUnit: 'tsp',
    secondaryValue: format(grams),
    secondaryUnit: 'g'
  };
}

/**
 * Reef Advantage Magnesium — dry, primary tsp.
 * Mirrors SeachemCalculations.calculateReefAdvantageMagnesium.
 */
export function calculateReefAdvantageMagnesium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const tspns = 0.0095 * volGal * diff;
  const grams = tspns * 5;
  return {
    primaryValue: format(tspns),
    primaryUnit: 'tsp',
    secondaryValue: format(grams),
    secondaryUnit: 'g'
  };
}

/**
 * Reef Advantage Strontium — dry, primary g.
 * Mirrors SeachemCalculations.calculateReefAdvantageStrontium.
 */
export function calculateReefAdvantageStrontium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const grams = (volGal * diff) / 7.5;
  const tspns = grams / 6;
  return {
    primaryValue: format(grams),
    primaryUnit: 'g',
    secondaryValue: format(tspns),
    secondaryUnit: 'tsp'
  };
}

/** Reef Calcium — liquid, mL primary. */
export function calculateReefCalcium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = (diff / 3) * (volGal / 20) * 5;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Reef Complete — liquid, mL primary. */
export function calculateReefComplete(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = 0.025 * volGal * diff;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Reef Fusion 1 (calcium component) — liquid, mL primary. */
export function calculateReefFusion1(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = (volGal / 6.5) * (diff / 4);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Reef Iodide — liquid, mL primary. */
export function calculateReefIodide(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = 0.5 * volGal * diff;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Reef Strontium — liquid, mL primary. */
export function calculateReefStrontium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = 0.4 * volGal * diff;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}
