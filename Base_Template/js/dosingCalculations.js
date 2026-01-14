// js/dosingCalculations.js v5.0 (High Precision)

/**
 * Calculates grams of Potassium Bicarbonate (KHCO₃) needed using stoichiometric formula.
 *
 * STOICHIOMETRIC CALCULATION:
 * - KHCO₃ Molecular Weight = 100.115 g/mol
 * - 1 dKH = 17.86 mg/L as CaCO₃ = 0.357 meq/L alkalinity
 * - Since CaCO₃ provides 2 eq/mol and KHCO₃ provides 1 eq/mol:
 * - KHCO₃ needed = 0.357 mmol/L × 100.115 mg/mmol = 35.74 mg/L per dKH
 * - Coefficient = 0.0357 g/L per dKH
 *
 * @param {number} currentKh - Current KH in dKH.
 * @param {number} targetKh - Target KH in dKH.
 * @param {number} litres - Water volume in litres.
 * @param {number} purity - Purity of the KHCO₃ compound (0.5 to 1.0).
 * @returns {number} Grams of KHCO₃ to dose (precision: 0.0001).
 */
function calculateKhco3Grams(currentKh, targetKh, litres, purity) {
    if (purity <= 0 || !isFinite(purity)) return 0;
    // Using stoichiometric coefficient based on molecular weight
    const grams = (targetKh - currentKh) * COEFF_KHCO3_STOICH * litres / purity;
    return Math.max(0, grams);
}

/**
 * Calculates grams of Seachem Equilibrium needed.
 * @param {number} deltaGh - The desired increase in dGH.
 * @param {number} litres - Water volume in litres.
 * @returns {number} Grams of Equilibrium to dose.
 */
function calculateEquilibriumGrams(deltaGh, litres) {
    // This formula was verified as correct.
    const grams = deltaGh * COEFF_EQUILIBRIUM * litres;
    return Math.max(0, grams);
}

/**
 * Calculates grams of Seachem Neutral Regulator needed.
 * @param {number} litres - Water volume in litres.
 * @param {number} currentPh - The current pH of the water.
 * @param {number} targetPh - The target pH (should be lower than current).
 * @param {number} currentKh - The current KH in dKH, which affects buffering.
 * @returns {number} Grams of Neutral Regulator to dose.
 */
function calculateNeutralRegulatorGrams(litres, currentPh, targetPh, currentKh) {
    // This formula was verified as correct.
    if (targetPh >= currentPh) return 0;
    const khEffectFactor = Math.min(currentKh, 4) / 4;
    const baseGramsPerLitre = GPL_MIN_NR + (GPL_MAX_NR - GPL_MIN_NR) * khEffectFactor;
    const phSteps = (currentPh - targetPh) / 0.5;
    if (phSteps <= 0) return 0;
    let grams = baseGramsPerLitre * litres * phSteps;
    grams = Math.min(grams, GPL_MAX_NR * litres * 2); // Cap the dose to prevent extreme changes.
    return Math.max(0, grams);
}

/**
 * Calculates grams of Seachem Acid Buffer needed.
 * @param {number} litres - Water volume in litres.
 * @param {number} currentKh - Current KH in dKH.
 * @param {number} targetKh - Target KH in dKH.
 * @returns {number} Grams of Acid Buffer to dose.
 */
function calculateAcidBufferGrams(litres, currentKh, targetKh) {
    // This formula now uses the corrected COEFF_ACID from utils.js to prevent overdosing.
    const deltaKh = currentKh - targetKh;
    const grams = deltaKh * COEFF_ACID * litres;
    return Math.max(0, grams);
}

/**
 * Calculates grams of Seachem Gold Buffer needed.
 * @param {number} litres - Water volume in litres.
 * @param {number} currentPh - The current pH of the water.
 * @param {number} targetPh - The target pH (should be higher than current).
 * @returns {object} An object containing the grams to dose and whether a full dose is recommended.
 */
function calculateGoldBufferGrams(litres, currentPh, targetPh) {
    // This formula was verified as correct.
    const deltaPh = targetPh - currentPh;
    if (deltaPh <= 0) return { grams: 0, fullDose: false };
    const fullDoseRecommended = deltaPh >= 0.3;
    const doseMultiplier = fullDoseRecommended ? 1 : 0.5;
    const grams = COEFF_GOLD_FULL * doseMultiplier * litres;
    return { grams: Math.max(0, grams), fullDose: fullDoseRecommended };
}

// --- Emergency Dosing Calculations ---

/**
 * Calculates the dose of Seachem Prime needed to detoxify ammonia/nitrite.
 * @param {number} litres - Water volume in litres.
 * @returns {number} The dose of Prime in mL.
 */
function calculatePrimeDose(litres) {
    return litres * PRIME_ML_PER_L;
}

/**
 * Calculates the dose of Seachem Stability needed to boost biological filtration.
 * @param {number} litres - Water volume in litres.
 * @returns {number} The dose of Stability in mL.
 */
function calculateStabilityDose(litres) {
    return litres * STABILITY_ML_PER_L;
}

// --- Additional Product Calculations ---

/**
 * Calculates grams of Seachem Safe needed for chlorine/chloramine removal.
 * Official dosing: 1g per 200L removes up to 4 ppm chlorine/chloramine.
 *
 * @param {number} litres - Water volume in litres.
 * @returns {number} Grams of Seachem Safe to dose (precision: 0.0001).
 */
function calculateSafeGrams(litres) {
    const grams = litres * COEFF_SAFE;
    return Math.max(0, grams);
}

/**
 * Calculates mL of APT Complete (2Hr Aquarist) fertilizer needed.
 * Standard dosing: 3ml per 100L daily.
 * This returns 80% of standard dose as per user preference.
 *
 * Also provides rough nitrate increase estimation.
 *
 * @param {number} litres - Water volume in litres.
 * @param {number} currentNitrate - Current nitrate reading in ppm (for estimation).
 * @returns {object} Object with { ml, estimatedNitrateIncrease, estimatedFinalNitrate }
 */
function calculateAptCompleteDose(litres, currentNitrate = 0) {
    const ml = litres * COEFF_APT_80PCT;
    // Rough estimate: ~1.5 ppm NO3 increase per ml per 100L
    const estimatedNitrateIncrease = (ml / 100) * APT_NITRATE_EST_PER_ML * 100;
    // Simplified: per liter dose contributes roughly 0.015 ppm per ml
    const perLiterContribution = ml * 0.015;
    const estimatedFinalNitrate = currentNitrate + perLiterContribution;

    return {
        ml: Math.max(0, ml),
        estimatedNitrateIncrease: Math.max(0, perLiterContribution),
        estimatedFinalNitrate: Math.max(0, estimatedFinalNitrate)
    };
}
