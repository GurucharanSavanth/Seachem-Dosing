import { describe, expect, it } from 'vitest';
import { calculateForProduct } from '../src/engine/index.js';
import { SALT_MIX_PRODUCTS, calculateSaltMix } from '../src/engine/salt-mix.js';
import { SUBSTRATE_PRODUCTS, calculateSubstrate } from '../src/engine/substrate.js';
import type { Product } from '../src/engine/types.js';

describe('engine.calculateForProduct dispatch', () => {
  const products: readonly Product[] = [
    'ALKALINE_BUFFER',
    'ACID_BUFFER',
    'EQUILIBRIUM',
    'POTASSIUM_BICARBONATE',
    'NEUTRAL_REGULATOR',
    'FLOURISH',
    'FLOURISH_IRON',
    'FLOURISH_NITROGEN',
    'FLOURISH_PHOSPHORUS',
    'FLOURISH_POTASSIUM',
    'FLOURISH_TRACE',
    'REEF_ADVANTAGE_CALCIUM',
    'REEF_ADVANTAGE_MAGNESIUM',
    'REEF_ADVANTAGE_STRONTIUM',
    'REEF_BUFFER',
    'REEF_BUILDER',
    'REEF_CARBONATE',
    'REEF_CALCIUM',
    'REEF_COMPLETE',
    'REEF_FUSION_1',
    'REEF_FUSION_2',
    'REEF_IODIDE',
    'REEF_STRONTIUM'
  ];

  it.each(products)('returns finite result for %s', (product) => {
    const result = calculateForProduct(product, 4, 6, 100, 'L', 'ppm');
    expect(Number.isFinite(result.primaryValue)).toBe(true);
    expect(Number.isFinite(result.secondaryValue)).toBe(true);
    expect(result.primaryUnit).toBeTruthy();
    expect(result.secondaryUnit).toBeTruthy();
  });

  it('FLOURISH ignores current/desired params', () => {
    const a = calculateForProduct('FLOURISH', 0, 0, 100, 'L', 'ppm');
    const b = calculateForProduct('FLOURISH', 999, 999, 100, 'L', 'ppm');
    expect(a.primaryValue).toBe(b.primaryValue);
  });
});

describe('SaltMix', () => {
  it('rejects desiredPpt <= currentPpt', () => {
    const r = calculateSaltMix('Red Sea Coral Pro', 5, 35, 34);
    expect(r).toBeNull();
  });

  it('rejects unknown product name', () => {
    const r = calculateSaltMix('Made Up Salt', 5, 0, 35);
    expect(r).toBeNull();
  });

  it('rejects desiredPpt > 50', () => {
    const r = calculateSaltMix('Red Sea Coral Pro', 5, 0, 51);
    expect(r).toBeNull();
  });

  it('returns positive grams for valid increase', () => {
    const r = calculateSaltMix('Red Sea Coral Pro', 5, 0, 35);
    expect(r).not.toBeNull();
    expect(r!.grams).toBeGreaterThan(0);
    expect(r!.kilograms).toBeCloseTo(r!.grams / 1000, 5);
    expect(r!.pounds).toBeGreaterThan(0);
  });

  it('exposes 17 products (matches Kotlin SaltMixCalculations)', () => {
    expect(Object.keys(SALT_MIX_PRODUCTS)).toHaveLength(17);
  });
});

describe('Substrate', () => {
  it('returns 0 bags for non-positive dimensions', () => {
    const r = calculateSubstrate(0, 30, 5, 'cm', 0);
    expect(r.bagsStandard).toBe(0);
  });

  it('exposes 11 product specs (matches Kotlin)', () => {
    expect(SUBSTRATE_PRODUCTS).toHaveLength(11);
  });

  it('clamps out-of-range product index', () => {
    const r1 = calculateSubstrate(60, 30, 5, 'cm', -10);
    const r2 = calculateSubstrate(60, 30, 5, 'cm', 0);
    expect(r1.bagsStandard).toBe(r2.bagsStandard);
  });

  it('cm input converts via /2.54 before applying divisor', () => {
    // 60 × 30 × 5 cm = 9000 cm³ → ~549.3 in³ ÷ 8250 → ceil(0.0666) = 1 bag
    const r = calculateSubstrate(60, 30, 5, 'cm', 0);
    expect(r.bagsStandard).toBe(1);
  });
});
