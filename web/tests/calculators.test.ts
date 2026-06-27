import { describe, expect, it } from 'vitest';
import {
  calculateAcidBufferGrams,
  calculateAptCompleteDose,
  calculateEquilibriumGrams,
  calculateGoldBufferGrams,
  calculateKhco3Grams,
  calculateNeutralRegulatorGrams,
  calculatePrimeDose,
  calculateSafeGrams,
  calculateStabilityDose
} from '../src/calculators/index.js';

describe('calculateKhco3Grams', () => {
  it('returns 0 for non-positive purity', () => {
    expect(calculateKhco3Grams(2, 4, 100, 0)).toBe(0);
    expect(calculateKhco3Grams(2, 4, 100, -0.5)).toBe(0);
  });

  it('returns 0 for non-finite purity', () => {
    expect(calculateKhco3Grams(2, 4, 100, Number.POSITIVE_INFINITY)).toBe(0);
    expect(calculateKhco3Grams(2, 4, 100, Number.NaN)).toBe(0);
  });

  it('clamps negative delta to 0', () => {
    expect(calculateKhco3Grams(4, 2, 100, 0.99)).toBe(0);
  });

  it('matches stoichiometric formula at unit purity', () => {
    // (4 - 2) * 0.0357 * 100 / 1.0 = 7.14
    expect(calculateKhco3Grams(2, 4, 100, 1.0)).toBeCloseTo(7.14, 5);
  });

  it('scales by 1/purity', () => {
    // base 7.14 / 0.99 = 7.21212...
    expect(calculateKhco3Grams(2, 4, 100, 0.99)).toBeCloseTo(7.21212, 4);
  });
});

describe('calculateEquilibriumGrams', () => {
  it('returns 0 for negative delta', () => {
    expect(calculateEquilibriumGrams(-2, 100)).toBe(0);
  });

  it('matches Seachem reference: 16 g per 80 L for 3 dGH', () => {
    expect(calculateEquilibriumGrams(3, 80)).toBeCloseTo(16, 5);
  });
});

describe('calculateNeutralRegulatorGrams', () => {
  it('returns 0 when target >= current pH', () => {
    expect(calculateNeutralRegulatorGrams(100, 7.0, 7.5, 4)).toBe(0);
    expect(calculateNeutralRegulatorGrams(100, 7.0, 7.0, 4)).toBe(0);
  });

  it('caps grams at GPL_MAX_NR * litres * 2', () => {
    // huge pH delta should clamp at 0.125 * 100 * 2 = 25
    const result = calculateNeutralRegulatorGrams(100, 9.0, 5.0, 10);
    expect(result).toBeLessThanOrEqual(25 + 1e-9);
  });

  it('zero KH effect factor at currentKh=0', () => {
    // grams = 0.0625 * 100 * (1 step) = 6.25 for 0.5 pH drop
    expect(calculateNeutralRegulatorGrams(100, 7.5, 7.0, 0)).toBeCloseTo(6.25, 5);
  });
});

describe('calculateAcidBufferGrams', () => {
  it('returns 0 for non-positive delta', () => {
    expect(calculateAcidBufferGrams(100, 4, 6)).toBe(0);
    expect(calculateAcidBufferGrams(100, 4, 4)).toBe(0);
  });

  it('matches Seachem reference: 1.5 g per 40 L for 2.8 dKH drop', () => {
    expect(calculateAcidBufferGrams(40, 5.8, 3.0)).toBeCloseTo(1.5, 5);
  });
});

describe('calculateGoldBufferGrams', () => {
  it('returns 0 for non-positive delta', () => {
    const r = calculateGoldBufferGrams(100, 7.5, 7.0);
    expect(r.grams).toBe(0);
    expect(r.fullDose).toBe(false);
  });

  it('full dose when delta >= 0.3', () => {
    const r = calculateGoldBufferGrams(40, 7.0, 7.5);
    expect(r.fullDose).toBe(true);
    // 6/40 * 1.0 * 40 = 6
    expect(r.grams).toBeCloseTo(6, 5);
  });

  it('half dose when delta < 0.3', () => {
    const r = calculateGoldBufferGrams(40, 7.0, 7.2);
    expect(r.fullDose).toBe(false);
    expect(r.grams).toBeCloseTo(3, 5);
  });
});

describe('calculateSafeGrams', () => {
  it('matches 1 g per 200 L', () => {
    expect(calculateSafeGrams(200)).toBeCloseTo(1, 5);
  });

  it('scales linearly', () => {
    expect(calculateSafeGrams(400)).toBeCloseTo(2, 5);
  });
});

describe('calculateAptCompleteDose', () => {
  it('returns 80% of standard 3 mL/100 L', () => {
    const r = calculateAptCompleteDose(100, 0);
    expect(r.ml).toBeCloseTo(2.4, 5);
  });

  it('estimates nitrate contribution above current', () => {
    const r = calculateAptCompleteDose(100, 10);
    expect(r.estimatedFinalNitrate).toBeGreaterThan(10);
    expect(r.estimatedNitrateIncrease).toBeGreaterThan(0);
  });

  it('clamps negative current nitrate to floor at the dose contribution', () => {
    const r = calculateAptCompleteDose(100, -5);
    expect(r.estimatedFinalNitrate).toBeGreaterThanOrEqual(0);
  });
});

describe('calculatePrimeDose', () => {
  it('matches 5 mL per 200 L', () => {
    expect(calculatePrimeDose(200)).toBeCloseTo(5, 5);
  });
});

describe('calculateStabilityDose', () => {
  it('matches 5 mL per 40 L', () => {
    expect(calculateStabilityDose(40)).toBeCloseTo(5, 5);
  });
});
