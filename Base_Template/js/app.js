// js/app.js v5.0

/**
 * Main calculation logic for dosing. Orchestrates input gathering,
 * validation, calculation, and result display.
 */
function doDosingCalculations() {
    const userInputs = getAllInputValues(); // From uiHandlers.js
    const errorMessages = [];
    const t = (key) => translations[currentLang][key] || key;

    // v5.0: Get effective volume based on mode (direct or LxBxH)
    const litres = getEffectiveVolumeLitres();

    // --- Validation ---
    if (!(litres > 0)) {
        errorMessages.push('Water Volume must be > 0');
    }
    if (userInputs.khPurity < 0.5 || userInputs.khPurity > 1) {
        errorMessages.push('KHCO₃ Purity must be between 0.50 and 1.00');
    }

    if (errorMessages.length > 0) {
        displayErrors(errorMessages);
        // Clear previous results if there are errors
        const zeroResults = {
            khDose: 0, equilibriumDose: 0, ghCurrentHigher: false, safeDose: 0,
            aptResult: { ml: 0, estimatedNitrateIncrease: 0, estimatedFinalNitrate: 0 },
            neutralRegulatorDose: 0, acidBufferDose: 0,
            goldBufferResult: { grams: 0, fullDose: false }
        };
        updateAllResults(zeroResults);
        return;
    }

    displayErrors([]); // Clear any previous errors

    const results = {};

    // --- Perform Calculations ---
    // KH Booster (KHCO3) - stoichiometric formula
    results.khDose = calculateKhco3Grams(userInputs.khCurrent, userInputs.khTarget, litres, userInputs.khPurity);

    // GH Booster (Equilibrium) - only raises GH
    const deltaGh = userInputs.ghTarget - userInputs.ghCurrent;
    results.ghCurrentHigher = userInputs.ghCurrent >= userInputs.ghTarget;
    results.equilibriumDose = deltaGh > 0 ? calculateEquilibriumGrams(deltaGh, litres) : 0;

    // v5.0: Seachem Safe
    results.safeDose = calculateSafeGrams(litres);

    // v5.0: APT Complete (uses nitrate from parameter status)
    const currentNitrate = userInputs.paramNitrate || 0;
    results.aptResult = calculateAptCompleteDose(litres, currentNitrate);

    // pH Neutralizer (Neutral Regulator)
    results.neutralRegulatorDose = calculateNeutralRegulatorGrams(litres, userInputs.phCurrent, userInputs.phTarget, userInputs.nrKh);

    // Acid Buffer
    results.acidBufferDose = calculateAcidBufferGrams(litres, userInputs.acidCurrentKh, userInputs.acidTargetKh);

    // Gold Buffer
    results.goldBufferResult = calculateGoldBufferGrams(litres, userInputs.phGoldCurrent, userInputs.phGoldTarget);

    // --- Display Results ---
    updateAllResults(results); // From uiHandlers.js
}


// --- App Initialization ---
document.addEventListener('DOMContentLoaded', () => {
    // Define the sequence of functions to run on user input or button clicks
    const calculationSequence = [handleParameterStatusUpdate, doDosingCalculations];

    // Initialize UI and pass the calculation sequence to the event listeners
    initUI(calculationSequence);

    // Run initial calculations on page load
    calculationSequence.forEach(cb => cb());
});
