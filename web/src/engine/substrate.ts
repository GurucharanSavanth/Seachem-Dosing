export type SubstrateUnit = 'cm' | 'in';

export interface SubstrateProductSpec {
  name: string;
  divisor: number;       // Volume in cubic inches per standard bag
  smallDivisor: number;  // Volume per small (7 kg) bag, or 0 if no small option
}

/**
 * Mirror of MainViewModel.calculateSubstrate's product spec table.
 * Volumes are in cubic inches per bag.
 */
export const SUBSTRATE_PRODUCTS: readonly SubstrateProductSpec[] = Object.freeze([
  { name: 'Flourite',            divisor: 8250,  smallDivisor: 9000 },
  { name: 'Flourite Black',      divisor: 7250,  smallDivisor: 0 },
  { name: 'Flourite Black Sand', divisor: 8000,  smallDivisor: 0 },
  { name: 'Flourite Dark',       divisor: 8250,  smallDivisor: 0 },
  { name: 'Flourite Red',        divisor: 8250,  smallDivisor: 0 },
  { name: 'Flourite Sand',       divisor: 8750,  smallDivisor: 0 },
  { name: 'Gray Coast',          divisor: 8500,  smallDivisor: 0 },
  { name: 'Meridian',            divisor: 10500, smallDivisor: 0 },
  { name: 'Onyx',                divisor: 8000,  smallDivisor: 0 },
  { name: 'Onyx Sand',           divisor: 8250,  smallDivisor: 0 },
  { name: 'Pearl Beach',         divisor: 9750,  smallDivisor: 0 }
]);

export interface SubstrateResult {
  bagsStandard: number;
  bagsSmall: number;
}

/**
 * Substrate (gravel) bag count.
 *
 * Mirrors SeachemCalculations.calculateGravel + MainViewModel.calculateSubstrate.
 *
 * @param length Tank dimension (cm or in)
 * @param width  Tank dimension
 * @param depth  Substrate depth
 * @param unit   'cm' converts to inches via /2.54 before applying divisor
 * @param productIndex Index into SUBSTRATE_PRODUCTS (clamped to valid range)
 */
export function calculateSubstrate(
  length: number,
  width: number,
  depth: number,
  unit: SubstrateUnit,
  productIndex: number
): SubstrateResult {
  if (length <= 0 || width <= 0 || depth <= 0) {
    return { bagsStandard: 0, bagsSmall: 0 };
  }
  const idx = Math.min(Math.max(productIndex, 0), SUBSTRATE_PRODUCTS.length - 1);
  const spec = SUBSTRATE_PRODUCTS[idx]!;

  let l = length;
  let w = width;
  let d = depth;
  if (unit === 'cm') {
    const inch = 2.54;
    l /= inch;
    w /= inch;
    d /= inch;
  }
  const volumeIn3 = l * w * d;

  if (spec.divisor <= 0) return { bagsStandard: 0, bagsSmall: 0 };

  const bagsStandard = Math.ceil(volumeIn3 / spec.divisor);
  const bagsSmall = spec.smallDivisor > 0 ? Math.ceil(volumeIn3 / spec.smallDivisor) : 0;

  return { bagsStandard, bagsSmall };
}
