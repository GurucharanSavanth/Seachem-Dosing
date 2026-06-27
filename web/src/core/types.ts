/**
 * Shared types for calculator outputs.
 *
 * Mirrors Kotlin `domain/model/DosingResult.kt` at the simpler-calculator layer.
 * The full BigDecimal-based product engine (per ADR-004 4a port from
 * SeachemCalculations.kt) lands in a later commit.
 */

export interface AptResult {
  ml: number;
  estimatedNitrateIncrease: number;
  estimatedFinalNitrate: number;
}

export interface GoldBufferResult {
  grams: number;
  fullDose: boolean;
}
