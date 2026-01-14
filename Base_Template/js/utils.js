// js/utils.js v5.0 (Precision Update)

// --- CONSTANTS ---
const US_GAL_TO_L = 3.78541;
const UK_GAL_TO_L = 4.54609;

// Dimension to volume conversions
const CM3_TO_L = 0.001;        // 1 cm³ = 0.001 L
const IN3_TO_L = 0.0163871;    // 1 in³ = 0.0163871 L
const FT3_TO_L = 28.3168;      // 1 ft³ = 28.3168 L

// KHCO3 - Stoichiometric calculation based on molecular weight
// KHCO3 MW = 100.115 g/mol
// 1 dKH = 17.86 mg/L CaCO3 = 0.357 meq/L alkalinity
// KHCO3 needed = 0.357 mmol/L × 100.115 mg/mmol = 35.74 mg/L per dKH
const KHCO3_MOLECULAR_WEIGHT = 100.115; // g/mol
const COEFF_KHCO3_STOICH = 0.0357;      // g/L per dKH (stoichiometric)

// Seachem Equilibrium - Official: 16g per 80L raises GH by 3 dH
const COEFF_EQUILIBRIUM = 16 / (80 * 3); // 0.0667 g/L per dGH - Verified correct

// Seachem Neutral Regulator
const GPL_MIN_NR = 0.0625; // Verified as correct
const GPL_MAX_NR = 0.125;  // Verified as correct

// Seachem Acid Buffer - Official: 1.5g per 40L for 2.8 dKH drop
const COEFF_ACID = 1.5 / (40 * 2.8); // 0.01339 g/L per dKH

// Seachem Gold Buffer - Official: 6g per 40L for full dose
const COEFF_GOLD_FULL = 6 / 40; // 0.15 g/L - Verified correct

// Seachem Safe - Official: 1g per 200L for chlorine/chloramine removal
const COEFF_SAFE = 1 / 200; // 0.005 g/L

// APT Complete (2Hr Aquarist) - 80% of standard 3ml/100L daily dose
const COEFF_APT_STANDARD = 3 / 100;     // 0.03 ml/L (100% dose)
const COEFF_APT_80PCT = 0.8 * (3 / 100); // 0.024 ml/L (80% dose)
const APT_NITRATE_EST_PER_ML = 1.5;      // Rough estimate: ~1.5 ppm NO3 per ml/100L

// Emergency dosing
const PPM_TO_DH = 17.86;
const PRIME_ML_PER_L = 5 / 200;    // 5mL per 200L -> 0.025 mL/L
const STABILITY_ML_PER_L = 5 / 40; // 5mL per 40L -> 0.125 mL/L

// Local Storage Keys
const THEME_KEY = 'theme_v5';
const LAST_UNIT_KEY = 'last_unit_v5';
const LANG_KEY = 'lang_v5';
const GH_UNIT_KEY = 'gh_unit_v5';
const KH_UNIT_KEY = 'kh_unit_v5';
const VOL_MODE_KEY = 'vol_mode_v5';  // 'direct' or 'lbh'
const DIM_UNIT_KEY = 'dim_unit_v5'; // 'cm', 'in', or 'ft'

// --- HELPER FUNCTIONS ---

/**
 * Shortcut for document.getElementById.
 * @param {string} id The ID of the element.
 * @returns {HTMLElement} The found element.
 */
function qs(id) {
    return document.getElementById(id);
}

/**
 * Safely parses a string to a float, returning 0 if invalid.
 * @param {string} x The string to parse.
 * @returns {number} The parsed float or 0.
 */
function parseFloatSafe(x) {
    const v = parseFloat(x);
    return isNaN(v) ? 0 : v;
}

/**
 * Converts volume to litres based on the selected unit.
 * @param {number} volume The volume value.
 * @param {string} unit The unit ('L', 'US', or 'UK').
 * @returns {number} The volume in litres.
 */
function toLitres(volume, unit) {
    if (unit === 'US') return volume * US_GAL_TO_L;
    if (unit === 'UK') return volume * UK_GAL_TO_L;
    return volume;
}

/**
 * Calculates volume in litres from dimensions (Length × Breadth × Height).
 * @param {number} length The length dimension.
 * @param {number} breadth The breadth/width dimension.
 * @param {number} height The height dimension.
 * @param {string} unit The dimension unit ('cm', 'in', or 'ft').
 * @returns {number} The volume in litres.
 */
function dimensionsToLitres(length, breadth, height, unit) {
    const volume = length * breadth * height;
    if (volume <= 0 || !isFinite(volume)) return 0;

    switch (unit) {
        case 'in': return volume * IN3_TO_L;
        case 'ft': return volume * FT3_TO_L;
        case 'cm':
        default: return volume * CM3_TO_L;
    }
}

/**
 * Converts ppm to degrees of hardness (dGH or dKH).
 * @param {number} ppm The value in parts per million.
 * @returns {number} The value in degrees of hardness.
 */
function ppmToDh(ppm) {
    if (ppm <= 0 || !isFinite(ppm)) return 0;
    return ppm / PPM_TO_DH;
}

/**
 * Converts degrees of hardness (dGH or dKH) to ppm.
 * @param {number} dh The value in degrees of hardness.
 * @returns {number} The value in parts per million.
 */
function dhToPpm(dh) {
    if (dh <= 0 || !isFinite(dh)) return 0;
    return dh * PPM_TO_DH;
}

/**
 * Formats a number to a specific number of decimal places.
 * @param {number} num The number to format.
 * @param {number} [places=2] The number of decimal places.
 * @returns {string} The formatted number as a string.
 */
function fmt(num, places = 2) {
    if (num < 0 || !isFinite(num)) return (0).toFixed(places);
    return num.toFixed(places);
}

/**
 * High-precision formatter for critical calculations.
 * @param {number} num The number to format.
 * @returns {string} The formatted number with 4 decimal places.
 */
function fmtPrecise(num) {
    return fmt(num, 4);
}

/**
 * Generates text for splitting a dose into two parts for safety.
 * @param {number} grams The total dose in grams.
 * @param {string} [lang='en'] The current language for translation.
 * @returns {string} The instructional text for splitting the dose.
 */
function splitText(grams, lang = 'en') {
    if (grams <= 0.01 || !isFinite(grams)) return '';
    const halfDose = fmt(grams / 2);
    if (lang === 'kn') {
        if (grams < 0.3) return 'ಒಂದೇ ಬಾರಿಗೆ ಡೋಸ್ ಮಾಡಿ';
        return `${halfDose} ಗ್ರಾಂ ಈಗ + ${halfDose} ಗ್ರಾಂ ೧೨-೨೪ ಗಂಟೆಗಳಲ್ಲಿ`;
    }
    if (grams < 0.3) return 'Dose in one go';
    return `${halfDose} g now + ${halfDose} g in 12–24 h`;
}

/**
 * Checks if the user's system prefers dark mode.
 * @returns {boolean} True if dark mode is preferred.
 */
function prefersDarkMode() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Applies the selected theme ('light' or 'dark') to the body.
 * @param {string} theme The theme to apply.
 */
function applyTheme(theme) {
    document.body.classList.toggle('dark', theme === 'dark');
    qs('themeToggle').textContent = theme === 'dark' ? '☀️' : '🌓';
}
