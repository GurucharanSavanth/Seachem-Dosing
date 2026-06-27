import {
  calculateAcidBuffer,
  calculateAlkalineBuffer,
  calculateEquilibrium,
  calculateNeutralRegulator,
  calculatePotassiumBicarbonate,
  calculateReefBuffer,
  calculateReefBuilder,
  calculateReefCarbonate,
  calculateReefFusion2
} from './buffers.js';
import {
  calculateFlourish,
  calculateFlourishIron,
  calculateFlourishNitrogen,
  calculateFlourishPhosphorus,
  calculateFlourishPotassium,
  calculateFlourishTrace
} from './flourish.js';
import {
  calculateReefAdvantageCalcium,
  calculateReefAdvantageMagnesium,
  calculateReefAdvantageStrontium,
  calculateReefCalcium,
  calculateReefComplete,
  calculateReefFusion1,
  calculateReefIodide,
  calculateReefStrontium
} from './reef-trace.js';
import type { EngineResult, Product, UnitScale, VolumeUnit } from './types.js';

export type { EngineResult, Product, UnitScale, VolumeUnit } from './types.js';
export * from './buffers.js';
export * from './flourish.js';
export * from './reef-trace.js';
export * from './substrate.js';
export * from './salt-mix.js';

const ZERO_RESULT: EngineResult = {
  primaryValue: 0,
  primaryUnit: 'g',
  secondaryValue: 0,
  secondaryUnit: 'tsp'
};

/**
 * Single dispatch entry point. Mirrors MainViewModel.calculateUniversal.
 *
 * Products that don't accept current/target inputs (FLOURISH, FLOURISH_TRACE)
 * ignore those parameters.
 */
export function calculateForProduct(
  product: Product,
  current: number,
  desired: number,
  volume: number,
  volumeUnit: VolumeUnit,
  scale: UnitScale = 'ppm'
): EngineResult {
  switch (product) {
    case 'ALKALINE_BUFFER':
      return calculateAlkalineBuffer(current, desired, volume, volumeUnit, scale);
    case 'ACID_BUFFER':
      return calculateAcidBuffer(current, desired, volume, volumeUnit, scale);
    case 'EQUILIBRIUM':
      return calculateEquilibrium(current, desired, volume, volumeUnit, scale);
    case 'POTASSIUM_BICARBONATE':
      return calculatePotassiumBicarbonate(current, desired, volume, volumeUnit, scale);
    case 'NEUTRAL_REGULATOR':
      return calculateNeutralRegulator(current, desired, volume, volumeUnit);
    case 'FLOURISH':
      return calculateFlourish(volume, volumeUnit);
    case 'FLOURISH_IRON':
      return calculateFlourishIron(current, desired, volume, volumeUnit, scale);
    case 'FLOURISH_NITROGEN':
      return calculateFlourishNitrogen(current, desired, volume, volumeUnit, scale);
    case 'FLOURISH_PHOSPHORUS':
      return calculateFlourishPhosphorus(current, desired, volume, volumeUnit, scale);
    case 'FLOURISH_POTASSIUM':
      return calculateFlourishPotassium(current, desired, volume, volumeUnit, scale);
    case 'FLOURISH_TRACE':
      return calculateFlourishTrace(volume, volumeUnit);
    case 'REEF_ADVANTAGE_CALCIUM':
      return calculateReefAdvantageCalcium(current, desired, volume, volumeUnit, scale);
    case 'REEF_ADVANTAGE_MAGNESIUM':
      return calculateReefAdvantageMagnesium(current, desired, volume, volumeUnit, scale);
    case 'REEF_ADVANTAGE_STRONTIUM':
      return calculateReefAdvantageStrontium(current, desired, volume, volumeUnit, scale);
    case 'REEF_BUFFER':
      return calculateReefBuffer(current, desired, volume, volumeUnit, scale);
    case 'REEF_BUILDER':
      return calculateReefBuilder(current, desired, volume, volumeUnit, scale);
    case 'REEF_CALCIUM':
      return calculateReefCalcium(current, desired, volume, volumeUnit, scale);
    case 'REEF_CARBONATE':
      return calculateReefCarbonate(current, desired, volume, volumeUnit, scale);
    case 'REEF_COMPLETE':
      return calculateReefComplete(current, desired, volume, volumeUnit, scale);
    case 'REEF_FUSION_1':
      return calculateReefFusion1(current, desired, volume, volumeUnit, scale);
    case 'REEF_FUSION_2':
      return calculateReefFusion2(current, desired, volume, volumeUnit, scale);
    case 'REEF_IODIDE':
      return calculateReefIodide(current, desired, volume, volumeUnit, scale);
    case 'REEF_STRONTIUM':
      return calculateReefStrontium(current, desired, volume, volumeUnit, scale);
    default:
      return ZERO_RESULT;
  }
}
