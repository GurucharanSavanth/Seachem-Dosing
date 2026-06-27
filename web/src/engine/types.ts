/**
 * Mirrors com.example.seachem_dosing.logic.SeachemCalculations
 * {Product, UnitScale, CalculationResult}.
 *
 * The "engine" layer (this folder) implements the BigDecimal-based product
 * calculator family from the Android side. The simpler Calculations.kt-aligned
 * functions live in src/calculators/ and stay in Number-precision lockstep
 * with Base_Template/js/dosingCalculations.js.
 */

export type UnitScale = 'meq/L' | 'dKH' | 'ppm' | 'pH';

export type VolumeUnit = 'L' | 'US' | 'UK';

export type Product =
  | 'ALKALINE_BUFFER'
  | 'ACID_BUFFER'
  | 'EQUILIBRIUM'
  | 'FLOURISH'
  | 'FLOURISH_IRON'
  | 'FLOURISH_NITROGEN'
  | 'FLOURISH_PHOSPHORUS'
  | 'FLOURISH_POTASSIUM'
  | 'FLOURISH_TRACE'
  | 'POTASSIUM_BICARBONATE'
  | 'NEUTRAL_REGULATOR'
  | 'REEF_ADVANTAGE_CALCIUM'
  | 'REEF_ADVANTAGE_MAGNESIUM'
  | 'REEF_ADVANTAGE_STRONTIUM'
  | 'REEF_BUFFER'
  | 'REEF_BUILDER'
  | 'REEF_CALCIUM'
  | 'REEF_CARBONATE'
  | 'REEF_COMPLETE'
  | 'REEF_FUSION_1'
  | 'REEF_FUSION_2'
  | 'REEF_IODIDE'
  | 'REEF_STRONTIUM';

export interface EngineResult {
  primaryValue: number;
  primaryUnit: string;
  secondaryValue: number;
  secondaryUnit: string;
}
