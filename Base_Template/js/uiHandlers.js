// js/uiHandlers.js v5.2 (Full v5.0 Support)

let allElements = {};
let currentLang = 'en';
let ghUnitMode = 'dh'; // 'dh' (degrees) or 'ppm'
let khUnitMode = 'dh'; // 'dh' (degrees) or 'ppm'
let volumeMode = 'direct'; // 'direct' or 'lbh'
let dimUnit = 'cm'; // 'cm', 'in', or 'ft'

/** Initializes all DOM element references into a central object. */
function initDOMReferences() {
    // This list is verified to be compatible with the revamped index.html v5.0
    const ids = [
        'themeToggle', 'langToggle', 'timestamp', 'errors', 'changelogModal', 'closeChangelog',
        'recommendations', 'paramAmmonia', 'statusAmmonia', 'paramNitrate', 'statusNitrate',
        'paramNitrite', 'statusNitrite', 'paramGh', 'statusGh', 'paramKh', 'statusKh',
        'ghUnit', 'khUnit', // Unit label spans for toggle
        'volume', 'unit', 'khCurrent', 'khTarget', 'khPurity', 'khco3Result', 'khSplit', 'copyKhco3',
        'ghCurrent', 'ghTarget', 'equilibriumResult', 'eqSplit', 'copyEquil',
        'phCurrent', 'phTarget', 'nrKh', 'neutralResult', 'nrSplit', 'copyNR',
        'acidCurrentKh', 'acidTargetKh', 'acidResult', 'acidSplit', 'copyAcid',
        'phGoldCurrent', 'phGoldTarget', 'goldResult', 'goldSplit', 'copyGold',
        // v5.0 new elements
        'menuToggle', 'menuOverlay', 'menuPanel', 'closeMenu',
        'menuThemeLight', 'menuThemeDark', 'menuLangEn', 'menuLangKn',
        'menuDownloadCsv', 'menuReset', 'menuChangelog',
        'directVolumeSection', 'lbhVolumeSection',
        'dimLength', 'dimBreadth', 'dimHeight', 'calculatedVolume',
        'safeResult', 'copySafe', 'aptResult', 'copyApt', 'nitrateEstimate'
    ];
    ids.forEach(id => { allElements[id] = qs(id); });
}

/** Populates the unit selection dropdown and sets the last used unit. */
function setupUnitSelection() {
    const units = { L: "Litres (L)", US: "US Gallons", UK: "UK Gallons" };
    Object.entries(units).forEach(([value, text]) => {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = text;
        allElements.unit.appendChild(option);
    });
    // v5.0: Default to US Gallons
    allElements.unit.value = localStorage.getItem(LAST_UNIT_KEY) || 'US';
    allElements.unit.addEventListener('change', () => localStorage.setItem(LAST_UNIT_KEY, allElements.unit.value));
}

/** Sets up the theme toggle functionality. */
function setupThemeToggle() {
    const storedTheme = localStorage.getItem(THEME_KEY) || (prefersDarkMode() ? 'dark' : 'light');
    applyTheme(storedTheme);
    allElements.themeToggle.addEventListener('click', () => {
        const newTheme = document.body.classList.contains('dark') ? 'light' : 'dark';
        applyTheme(newTheme);
        localStorage.setItem(THEME_KEY, newTheme);
    });
}

/** Sets up the language toggle functionality. */
function setupLanguageToggle() {
    currentLang = localStorage.getItem(LANG_KEY) || 'en';
    translatePage();
    allElements.langToggle.addEventListener('click', () => {
        currentLang = currentLang === 'en' ? 'kn' : 'en';
        translatePage();
        localStorage.setItem(LANG_KEY, currentLang);
    });
}

/**
 * Sets up GH/KH unit toggle functionality.
 * Loads saved preferences from localStorage and attaches event listeners.
 */
function setupUnitToggles() {
    // Load saved preferences (default is 'dh' for degrees)
    ghUnitMode = localStorage.getItem(GH_UNIT_KEY) || 'dh';
    khUnitMode = localStorage.getItem(KH_UNIT_KEY) || 'dh';

    // Apply initial state to buttons and labels
    applyUnitToggleState('gh', ghUnitMode);
    applyUnitToggleState('kh', khUnitMode);

    // Attach click handlers to all toggle buttons
    document.querySelectorAll('.unit-toggle-btn').forEach(btn => {
        btn.addEventListener('click', handleUnitToggle);
    });
}

/**
 * Handles unit toggle button clicks.
 * Converts the current value and updates the UI.
 * @param {Event} event The click event.
 */
function handleUnitToggle(event) {
    const btn = event.target;
    const param = btn.dataset.param; // 'gh' or 'kh'
    const newUnit = btn.dataset.unit; // 'dh' or 'ppm'

    // Get current mode and input element
    const currentMode = param === 'gh' ? ghUnitMode : khUnitMode;
    const inputEl = allElements[param === 'gh' ? 'paramGh' : 'paramKh'];
    const currentValue = parseFloatSafe(inputEl.value);

    // Skip if already in this mode
    if (currentMode === newUnit) return;

    // Convert the value
    let newValue;
    if (newUnit === 'ppm') {
        // Converting from degrees to ppm
        newValue = Math.round(dhToPpm(currentValue));
    } else {
        // Converting from ppm to degrees
        newValue = Math.round(ppmToDh(currentValue));
    }

    // Update input value
    inputEl.value = newValue;

    // Update state
    if (param === 'gh') {
        ghUnitMode = newUnit;
        localStorage.setItem(GH_UNIT_KEY, newUnit);
    } else {
        khUnitMode = newUnit;
        localStorage.setItem(KH_UNIT_KEY, newUnit);
    }

    // Update UI (buttons and label)
    applyUnitToggleState(param, newUnit);

    // Trigger recalculation
    handleParameterStatusUpdate();
    doDosingCalculations();
}

/**
 * Applies visual state to toggle buttons and unit label.
 * @param {string} param 'gh' or 'kh'
 * @param {string} activeUnit 'dh' or 'ppm'
 */
function applyUnitToggleState(param, activeUnit) {
    // Update button active states and aria-pressed
    document.querySelectorAll(`.unit-toggle-btn[data-param="${param}"]`).forEach(btn => {
        const isActive = btn.dataset.unit === activeUnit;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-pressed', isActive);
    });

    // Update unit label text
    const unitEl = allElements[param === 'gh' ? 'ghUnit' : 'khUnit'];
    if (unitEl) {
        if (activeUnit === 'ppm') {
            unitEl.textContent = 'ppm';
        } else {
            unitEl.textContent = param === 'gh' ? 'dGH' : 'dKH';
        }
    }
}

/**
 * Sets up the hamburger menu functionality.
 */
function setupHamburgerMenu() {
    const menuToggle = allElements.menuToggle;
    const menuOverlay = allElements.menuOverlay;
    const menuPanel = allElements.menuPanel;
    const closeMenu = allElements.closeMenu;

    function openMenu() {
        menuPanel.classList.add('active');
        menuOverlay.classList.add('active');
        menuToggle.setAttribute('aria-expanded', 'true');
        menuPanel.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
    }

    function closeMenuFn() {
        menuPanel.classList.remove('active');
        menuOverlay.classList.remove('active');
        menuToggle.setAttribute('aria-expanded', 'false');
        menuPanel.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    }

    if (menuToggle) menuToggle.addEventListener('click', openMenu);
    if (closeMenu) closeMenu.addEventListener('click', closeMenuFn);
    if (menuOverlay) menuOverlay.addEventListener('click', closeMenuFn);

    // Sync theme buttons in menu
    syncMenuThemeButtons();
    syncMenuLangButtons();

    // Theme toggle buttons in menu
    if (allElements.menuThemeLight) {
        allElements.menuThemeLight.addEventListener('click', () => {
            applyTheme('light');
            localStorage.setItem(THEME_KEY, 'light');
            syncMenuThemeButtons();
        });
    }
    if (allElements.menuThemeDark) {
        allElements.menuThemeDark.addEventListener('click', () => {
            applyTheme('dark');
            localStorage.setItem(THEME_KEY, 'dark');
            syncMenuThemeButtons();
        });
    }

    // Language toggle buttons in menu
    if (allElements.menuLangEn) {
        allElements.menuLangEn.addEventListener('click', () => {
            currentLang = 'en';
            translatePage();
            localStorage.setItem(LANG_KEY, currentLang);
            syncMenuLangButtons();
        });
    }
    if (allElements.menuLangKn) {
        allElements.menuLangKn.addEventListener('click', () => {
            currentLang = 'kn';
            translatePage();
            localStorage.setItem(LANG_KEY, currentLang);
            syncMenuLangButtons();
        });
    }

    // Menu action buttons
    if (allElements.menuDownloadCsv) {
        allElements.menuDownloadCsv.addEventListener('click', () => {
            handleCsvDownload();
            closeMenuFn();
        });
    }
    if (allElements.menuReset) {
        allElements.menuReset.addEventListener('click', () => {
            handleReset([handleParameterStatusUpdate, doDosingCalculations]);
            closeMenuFn();
        });
    }
    if (allElements.menuChangelog) {
        allElements.menuChangelog.addEventListener('click', () => {
            allElements.changelogModal.style.display = 'flex';
            closeMenuFn();
        });
    }

    // Buffer links - show hidden sections and scroll to card
    document.querySelectorAll('.menu-link[data-card]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const cardId = link.dataset.card;
            const card = document.getElementById(cardId);
            if (card) {
                closeMenuFn();
                setTimeout(() => {
                    // Show the Advanced Buffers section divider
                    const advSection = document.getElementById('advancedBuffersSection');
                    if (advSection) advSection.classList.add('visible');

                    // Show all advanced buffer cards
                    document.querySelectorAll('.advanced-buffer').forEach(buffer => {
                        buffer.classList.add('visible');
                    });

                    // Scroll to the specific card and open it
                    card.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    const details = card.querySelector('details');
                    if (details) details.open = true;
                }, 300);
            }
        });
    });

    // Close menu on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && menuPanel.classList.contains('active')) {
            closeMenuFn();
        }
    });
}

/**
 * Syncs menu theme buttons to reflect current theme.
 */
function syncMenuThemeButtons() {
    const isDark = document.body.classList.contains('dark');
    if (allElements.menuThemeLight) {
        allElements.menuThemeLight.classList.toggle('active', !isDark);
    }
    if (allElements.menuThemeDark) {
        allElements.menuThemeDark.classList.toggle('active', isDark);
    }
}

/**
 * Syncs menu language buttons to reflect current language.
 */
function syncMenuLangButtons() {
    if (allElements.menuLangEn) {
        allElements.menuLangEn.classList.toggle('active', currentLang === 'en');
    }
    if (allElements.menuLangKn) {
        allElements.menuLangKn.classList.toggle('active', currentLang === 'kn');
    }
}

/**
 * Sets up volume mode toggle (Direct Entry vs L×B×H).
 */
function setupVolumeModeToggle() {
    // Load saved preference
    volumeMode = localStorage.getItem(VOL_MODE_KEY) || 'direct';
    dimUnit = localStorage.getItem(DIM_UNIT_KEY) || 'cm';

    applyVolumeModeState();
    applyDimUnitState();

    // Volume mode toggle buttons
    document.querySelectorAll('.vol-mode-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const mode = e.target.dataset.mode;
            if (mode !== volumeMode) {
                volumeMode = mode;
                localStorage.setItem(VOL_MODE_KEY, mode);
                applyVolumeModeState();
                // Trigger recalculation
                if (mode === 'lbh') {
                    calculateVolumeFromDimensions();
                }
                doDosingCalculations();
            }
        });
    });

    // Dimension unit toggle buttons
    document.querySelectorAll('.dim-unit-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const unit = e.target.dataset.unit;
            if (unit !== dimUnit) {
                dimUnit = unit;
                localStorage.setItem(DIM_UNIT_KEY, unit);
                applyDimUnitState();
                calculateVolumeFromDimensions();
                doDosingCalculations();
            }
        });
    });

    // Dimension inputs
    ['dimLength', 'dimBreadth', 'dimHeight'].forEach(id => {
        if (allElements[id]) {
            allElements[id].addEventListener('input', () => {
                calculateVolumeFromDimensions();
                doDosingCalculations();
            });
        }
    });
}

/**
 * Applies visual state for volume mode toggle.
 */
function applyVolumeModeState() {
    // Update buttons
    document.querySelectorAll('.vol-mode-btn').forEach(btn => {
        const isActive = btn.dataset.mode === volumeMode;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-pressed', isActive);
    });

    // Show/hide sections
    if (allElements.directVolumeSection) {
        allElements.directVolumeSection.style.display = volumeMode === 'direct' ? 'block' : 'none';
    }
    if (allElements.lbhVolumeSection) {
        allElements.lbhVolumeSection.style.display = volumeMode === 'lbh' ? 'block' : 'none';
    }
}

/**
 * Applies visual state for dimension unit toggle.
 */
function applyDimUnitState() {
    document.querySelectorAll('.dim-unit-btn').forEach(btn => {
        const isActive = btn.dataset.unit === dimUnit;
        btn.classList.toggle('active', isActive);
        btn.setAttribute('aria-pressed', isActive);
    });
}

/**
 * Calculates volume from L×B×H dimensions.
 */
function calculateVolumeFromDimensions() {
    const length = parseFloatSafe(allElements.dimLength?.value);
    const breadth = parseFloatSafe(allElements.dimBreadth?.value);
    const height = parseFloatSafe(allElements.dimHeight?.value);

    const litres = dimensionsToLitres(length, breadth, height, dimUnit);

    if (allElements.calculatedVolume) {
        allElements.calculatedVolume.textContent = fmt(litres, 2);
    }
}

/**
 * Gets the effective volume in litres based on current mode.
 * @returns {number} Volume in litres.
 */
function getEffectiveVolumeLitres() {
    if (volumeMode === 'lbh') {
        const length = parseFloatSafe(allElements.dimLength?.value);
        const breadth = parseFloatSafe(allElements.dimBreadth?.value);
        const height = parseFloatSafe(allElements.dimHeight?.value);
        return dimensionsToLitres(length, breadth, height, dimUnit);
    } else {
        const volume = parseFloatSafe(allElements.volume?.value);
        const unit = allElements.unit?.value || 'L';
        return toLitres(volume, unit);
    }
}

/** Translates the entire page using data-lang-key attributes. */
function translatePage() {
    document.querySelectorAll('[data-lang-key]').forEach(el => {
        const key = el.dataset.langKey;
        const translation = translations[currentLang][key];

        if (translation) {
            // SECURITY REFACTOR from original code is preserved.
            // It's safer because we explicitly control which keys can contain HTML.
            const isHtmlContent = key.startsWith('reco_') || key === 'changelog_list';
            if (isHtmlContent) {
                 el.innerHTML = translation; // Use innerHTML only for specific, trusted keys.
            } else {
                el.textContent = translation; // Default to the safer textContent.
            }
        }
    });
    // Manually trigger a recalculation to update dynamic result text.
    handleParameterStatusUpdate(); // This needs to run before doDosingCalculations
    doDosingCalculations();
}


/** Collects all input values from the DOM. */
function getAllInputValues() {
    const inputs = {};
    const numericKeys = [
        'paramAmmonia', 'paramNitrate', 'paramNitrite', 'paramGh', 'paramKh', 'volume',
        'khCurrent', 'khTarget', 'khPurity', 'ghCurrent', 'ghTarget',
        'phCurrent', 'phTarget', 'nrKh', 'acidCurrentKh', 'acidTargetKh',
        'phGoldCurrent', 'phGoldTarget'
    ];
    for (const key in allElements) {
        if (allElements[key] instanceof HTMLInputElement || allElements[key] instanceof HTMLSelectElement) {
            inputs[key] = numericKeys.includes(key) ? parseFloatSafe(allElements[key].value) : allElements[key].value;
        }
    }
    return inputs;
}

/** Displays validation errors or clears them. */
function displayErrors(errorMessages) {
    allElements.errors.innerHTML = errorMessages.length > 0 ? errorMessages.join(' &bull; ') : '';
    if (errorMessages.length > 0) allElements.errors.focus();
}

/** Updates UI based on parameter status checks. */
function handleParameterStatusUpdate() {
    const inputs = getAllInputValues();
    // v5.0: Use effective volume based on mode (direct or LxBxH)
    const litres = getEffectiveVolumeLitres();
    let recommendations = [];
    const t = (key, replacements = {}) => {
        let text = translations[currentLang][key] || key;
        for(const r in replacements) {
            text = text.replace(`{${r}}`, replacements[r]);
        }
        return text;
    };
    
    const updateStatus = (element, className, textKey) => {
        element.className = 'status-badge ' + className;
        element.textContent = t(textKey);
    };

    // Ammonia
    if (inputs.paramAmmonia > 0) {
        updateStatus(allElements.statusAmmonia, 'warn', 'status_danger');
        recommendations.push(t('reco_ammonia_detected'));
        if (litres > 0) {
            const primeDose = calculatePrimeDose(litres);
            recommendations.push(t('reco_prime_dose', {primeDose: fmt(primeDose)}));
        } else {
            recommendations.push(t('reco_volume_needed'));
        }
    } else {
        updateStatus(allElements.statusAmmonia, 'good', 'status_good');
    }

    // Nitrite
    if (inputs.paramNitrite > 0) {
        updateStatus(allElements.statusNitrite, 'warn', 'status_danger');
        recommendations.push(t('reco_nitrite_detected'));
        if (litres > 0) {
            const stabilityDose = calculateStabilityDose(litres);
            recommendations.push(t('reco_stability_dose', {stabilityDose: fmt(stabilityDose)}));
        }
    } else {
        updateStatus(allElements.statusNitrite, 'good', 'status_good');
    }

    // Nitrate
    if (inputs.paramNitrate > 50) {
        updateStatus(allElements.statusNitrate, 'warn', 'status_high');
        recommendations.push(t('reco_nitrate_high'));
    } else {
        updateStatus(allElements.statusNitrate, 'good', 'status_good');
    }
    
    // GH - Check unit mode before conversion
    let dGH;
    if (ghUnitMode === 'ppm') {
        // User entered PPM, convert to degrees
        dGH = ppmToDh(inputs.paramGh);
    } else {
        // User entered degrees, use directly
        dGH = inputs.paramGh;
    }
    allElements.statusGh.className = 'status-badge'; // It's just info
    allElements.statusGh.textContent = `${fmt(dGH, 1)} °dGH`;
    allElements.ghCurrent.value = fmt(dGH, 2);
    if (dGH < 3 && dGH > 0) { // Using dGH for recommendation
        recommendations.push(t('reco_gh_low'));
    }

    // KH - Check unit mode before conversion
    let dKH;
    if (khUnitMode === 'ppm') {
        // User entered PPM, convert to degrees
        dKH = ppmToDh(inputs.paramKh);
    } else {
        // User entered degrees, use directly
        dKH = inputs.paramKh;
    }
    allElements.statusKh.className = 'status-badge'; // It's just info
    allElements.statusKh.textContent = `${fmt(dKH, 1)} °dKH`;
    [allElements.khCurrent, allElements.nrKh, allElements.acidCurrentKh].forEach(el => el.value = fmt(dKH, 2));
    if (dKH < 3 && dKH > 0) { // Using dKH for recommendation
        recommendations.push(t('reco_kh_low'));
    }

    // Build recommendation list using DOM methods
    allElements.recommendations.innerHTML = ''; // Clear previous
    if (recommendations.length > 0) {
        recommendations.forEach(rec => {
            const p = document.createElement('p');
            p.innerHTML = rec; // Using innerHTML as translations contain <strong>
            allElements.recommendations.appendChild(p);
        });
    } else {
        const p = document.createElement('p');
        p.innerHTML = t('reco_ok');
        allElements.recommendations.appendChild(p);
    }
}

/** Updates all result fields in the UI. */
function updateAllResults(results) {
    const t = (key) => translations[currentLang][key] || key;

    // KH Booster (KHCO3)
    allElements.khco3Result.textContent = `${fmtPrecise(results.khDose)} g KHCO₃`;
    allElements.khco3Result.dataset.dose = fmtPrecise(results.khDose);
    allElements.khSplit.textContent = splitText(results.khDose, currentLang);

    // GH Booster (Equilibrium) - only raises GH, shows message if current >= target
    if (results.equilibriumDose > 0) {
        allElements.equilibriumResult.textContent = `${fmtPrecise(results.equilibriumDose)} g Equilibrium`;
        allElements.eqSplit.textContent = splitText(results.equilibriumDose, currentLang);
    } else if (results.ghCurrentHigher) {
        allElements.equilibriumResult.textContent = 'GH already at/above target';
        allElements.eqSplit.textContent = 'Equilibrium raises GH, not lowers it';
    } else {
        allElements.equilibriumResult.textContent = t('no_dose_needed');
        allElements.eqSplit.textContent = '';
    }
    allElements.equilibriumResult.dataset.dose = fmtPrecise(results.equilibriumDose);

    // Seachem Safe (v5.0)
    if (allElements.safeResult) {
        allElements.safeResult.textContent = `${fmtPrecise(results.safeDose)} g Safe`;
        allElements.safeResult.dataset.dose = fmtPrecise(results.safeDose);
    }

    // APT Complete (v5.0)
    if (allElements.aptResult) {
        allElements.aptResult.textContent = `${fmt(results.aptResult.ml, 2)} ml APT Complete`;
        allElements.aptResult.dataset.dose = fmt(results.aptResult.ml, 4);
    }
    if (allElements.nitrateEstimate) {
        allElements.nitrateEstimate.textContent = `Est. NO₃ increase: +${fmt(results.aptResult.estimatedNitrateIncrease, 2)} ppm`;
    }

    // pH Neutralizer (Neutral Regulator)
    allElements.neutralResult.textContent = results.neutralRegulatorDose > 0 ? `${fmtPrecise(results.neutralRegulatorDose)} g Neutral Reg.` : t('no_dose_needed');
    allElements.neutralResult.dataset.dose = fmtPrecise(results.neutralRegulatorDose);
    allElements.nrSplit.textContent = splitText(results.neutralRegulatorDose, currentLang);

    // Acid Buffer
    allElements.acidResult.textContent = results.acidBufferDose > 0 ? `${fmtPrecise(results.acidBufferDose)} g Acid Buffer` : t('no_dose_needed');
    allElements.acidResult.dataset.dose = fmtPrecise(results.acidBufferDose);
    allElements.acidSplit.textContent = splitText(results.acidBufferDose, currentLang);

    // Gold Buffer
    const goldText = results.goldBufferResult.grams > 0 ? `${fmtPrecise(results.goldBufferResult.grams)} g Gold Buffer (${results.goldBufferResult.fullDose ? 'full' : 'half'} dose)` : t('no_dose_needed');
    allElements.goldResult.textContent = goldText;
    allElements.goldResult.dataset.dose = fmtPrecise(results.goldBufferResult.grams);
    allElements.goldSplit.textContent = splitText(results.goldBufferResult.grams, currentLang);

    updateTimestamp();
}

/** Updates the timestamp display. */
function updateTimestamp() {
    const ts = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    allElements.timestamp.textContent = `Last calc: ${ts}`;
}

/** Resets all inputs to default and recalculates. */
function handleReset(callbacks) {
    document.querySelectorAll('input[type=number]').forEach(input => { input.value = input.defaultValue || 0; });
    document.querySelectorAll('input.param-input').forEach(input => { input.value = input.defaultValue || 0; });
    allElements.volume.value = 10;
    allElements.khPurity.value = 0.99;

    // v5.0: Default to US Gallons
    allElements.unit.value = 'US';
    localStorage.setItem(LAST_UNIT_KEY, 'US');

    // Reset GH/KH unit toggles to degrees (default)
    ghUnitMode = 'dh';
    khUnitMode = 'dh';
    localStorage.setItem(GH_UNIT_KEY, 'dh');
    localStorage.setItem(KH_UNIT_KEY, 'dh');
    applyUnitToggleState('gh', 'dh');
    applyUnitToggleState('kh', 'dh');

    // Reset GH/KH to default degree values
    allElements.paramGh.value = 4;
    allElements.paramKh.value = 6;

    // v5.0: Reset volume mode to direct
    volumeMode = 'direct';
    localStorage.setItem(VOL_MODE_KEY, 'direct');
    applyVolumeModeState();

    // v5.0: Reset dimension unit to cm
    dimUnit = 'cm';
    localStorage.setItem(DIM_UNIT_KEY, 'cm');
    applyDimUnitState();

    // Reset LxBxH inputs
    if (allElements.dimLength) allElements.dimLength.value = 60;
    if (allElements.dimBreadth) allElements.dimBreadth.value = 30;
    if (allElements.dimHeight) allElements.dimHeight.value = 40;
    calculateVolumeFromDimensions();

    // v5.0: Hide Advanced Buffers section again
    const advSection = document.getElementById('advancedBuffersSection');
    if (advSection) advSection.classList.remove('visible');
    document.querySelectorAll('.advanced-buffer').forEach(buffer => {
        buffer.classList.remove('visible');
    });

    callbacks.forEach(cb => cb());
}

/** Generates and downloads a CSV of the results. */
function handleCsvDownload() {
    const rows = [
        ['Parameter', 'Dose', 'Unit'],
        ['KHCO3', allElements.khco3Result.dataset.dose || '0', 'g'],
        ['Equilibrium', allElements.equilibriumResult.dataset.dose || '0', 'g'],
        ['Safe', allElements.safeResult?.dataset.dose || '0', 'g'],
        ['APT Complete', allElements.aptResult?.dataset.dose || '0', 'ml'],
        ['Neutral Regulator', allElements.neutralResult.dataset.dose || '0', 'g'],
        ['Acid Buffer', allElements.acidResult.dataset.dose || '0', 'g'],
        ['Gold Buffer', allElements.goldResult.dataset.dose || '0', 'g'],
    ];
    const csvContent = "data:text/csv;charset=utf-8," + rows.map(r => r.join(',')).join('\r\n');
    const link = document.createElement('a');
    link.href = encodeURI(csvContent);
    link.download = `aquarium-dosing-results-${new Date().toISOString().slice(0,10)}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/** Sets up clipboard copy functionality. */
function setupCopyButtons() {
    const copyMap = {
        copyKhco3: 'khco3Result',
        copyEquil: 'equilibriumResult',
        copySafe: 'safeResult',
        copyApt: 'aptResult',
        copyNR: 'neutralResult',
        copyAcid: 'acidResult',
        copyGold: 'goldResult'
    };
    for (const btnId in copyMap) {
        const button = allElements[btnId];
        const resultEl = allElements[copyMap[btnId]];
        if (button && resultEl) {
            button.addEventListener('click', (e) => {
                e.stopPropagation();
                const textToCopy = resultEl.dataset.dose || '0';
                navigator.clipboard.writeText(textToCopy).then(() => {
                    const icon = button.querySelector('span');
                    if (icon) {
                        icon.textContent = '✔️';
                        setTimeout(() => { icon.textContent = '📋'; }, 1200);
                    }
                });
            });
        }
    }
}

/** Sets up modal functionality. */
function setupModal() {
    // v5.0: Changelog is now accessed via hamburger menu, but keep modal functionality
    if (allElements.closeChangelog) {
        allElements.closeChangelog.addEventListener('click', () => allElements.changelogModal.style.display = 'none');
    }
    if (allElements.changelogModal) {
        allElements.changelogModal.addEventListener('click', (e) => {
            if (e.target === allElements.changelogModal) allElements.changelogModal.style.display = 'none';
        });
    }
}

/** Initializes all UI event listeners. */
function initEventListeners(callbacks) {
    let debounceTimeout;
    // v5.0: Auto-calculate on any input change (no Calculate button needed)
    document.body.addEventListener('input', (event) => {
        if (event.target instanceof HTMLInputElement || event.target instanceof HTMLSelectElement) {
            clearTimeout(debounceTimeout);
            debounceTimeout = setTimeout(() => { callbacks.forEach(cb => cb()); }, 250);
        }
    });

    // v5.0: Footer buttons removed, actions now in hamburger menu
    // Keep compatibility if buttons still exist
    if (allElements.btnCalc) {
        allElements.btnCalc.addEventListener('click', () => callbacks.forEach(cb => cb()));
    }
    if (allElements.btnReset) {
        allElements.btnReset.addEventListener('click', () => handleReset(callbacks));
    }
    if (allElements.btnCsv) {
        allElements.btnCsv.addEventListener('click', handleCsvDownload);
    }

    setupCopyButtons();
    setupModal();
}

/** Main UI initialization function. */
function initUI(callbacks) {
    initDOMReferences();
    setupUnitSelection();
    setupThemeToggle();
    setupLanguageToggle();
    setupUnitToggles();
    setupHamburgerMenu();       // v5.0
    setupVolumeModeToggle();    // v5.0
    calculateVolumeFromDimensions(); // v5.0: Initialize calculated volume
    initEventListeners(callbacks);
}
