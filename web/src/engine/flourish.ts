import { convertInput, format, toUsGal } from './conversions.js';
import type { EngineResult, UnitScale, VolumeUnit } from './types.js';

function diffPpm(current: number, desired: number, scale: UnitScale): number {
  const curPpm = convertInput(current, scale, 'ppm');
  const desPpm = convertInput(desired, scale, 'ppm');
  return desPpm - curPpm;
}

/**
 * Flourish (general) — volume-only, no current/target.
 * Mirrors SeachemCalculations.calculateFlourish.
 */
export function calculateFlourish(volume: number, volumeUnit: VolumeUnit): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const ml = (volGal / 60) * 5;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/**
 * Flourish Trace — volume-only, no current/target.
 * Mirrors SeachemCalculations.calculateFlourishTrace.
 */
export function calculateFlourishTrace(volume: number, volumeUnit: VolumeUnit): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const ml = (volGal / 20) * 5;
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Flourish Iron — ppm base. */
export function calculateFlourishIron(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = (volGal / 50) * (diff * 20);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Flourish Nitrogen — ppm base. */
export function calculateFlourishNitrogen(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = diff * (volGal * 0.25);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Flourish Phosphorus — ppm base. */
export function calculateFlourishPhosphorus(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = (volGal / 20) * (diff * 16.6);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}

/** Flourish Potassium — ppm base. */
export function calculateFlourishPotassium(
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale
): EngineResult {
  const volGal = toUsGal(volume, volumeUnit);
  const diff = diffPpm(current, desired, scale);
  const ml = (volGal / 30) * (diff * 2.5);
  const caps = ml / 5;
  return {
    primaryValue: format(ml),
    primaryUnit: 'mL',
    secondaryValue: format(caps),
    secondaryUnit: 'caps'
  };
}
