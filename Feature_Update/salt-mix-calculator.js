/*  Salt Mix Calculator (extracted from Bulk Reef Supply "Salt Mix Calculator")
    Inputs:
      - productName (string)
      - volumeGallons (US gallons)
      - currentPpt (PPT)
      - desiredPpt (PPT)
      - temperatureC (°C) accepted for your UI, but NOT used in the BRS PPT-based mass formula
    Output:
      - grams + kilograms (also pounds for cross-check)

    Source logic:
      BRS computes lbs as:
        (desired - current) * volume * productFactor * 0.00220462
      where 0.00220462 converts grams -> pounds.
*/

(function (root, factory) {
  "use strict";
  if (typeof module === "object" && module.exports) {
    module.exports = factory();
  } else {
    root.SaltMixCalculator = factory();
  }
})(typeof self !== "undefined" ? self : this, function () {
  "use strict";

  // Extracted "Choose a product" options and their exact values (factors)
  // Factor unit (as implied by BRS code): grams needed per (gallon * 1 PPT)
  const SALT_MIX_PRODUCTS = Object.freeze({
    "Aquaforest Hybrid Pro Salt Mix": 4.3769,
    "Aquaforest Reef Salt Mix": 4.3769,
    "Aquaforest Reef Salt Plus Mix": 4.3769,
    "Aquaforest Sea Salt Mix": 4.3769,
    "Brightwell NeoMarine": 3.971428571,
    "HW-Marinemix Professional": 3.857142857,
    "HW-Marinemix Reefer": 3.857142857,
    "Instant Ocean Sea Salt Mix": 4.285714286,
    "Instant Ocean Reef Crystals": 4.285714286,
    "Nyos Pure Salt Mix": 4.339,
    "Red Sea Coral Pro": 4.114285714,
    "Red Sea Blue Bucket": 4.114285714,
    "Reef Crystals": 4.285714286,
    "Tropic Marin Bio-Actif": 4.2,
    "Tropic Marin Classic": 4.2,
    "Tropic Marin Pro Reef": 4.2,
    "Tropic Marin SynBiotic": 4.2
  });

  // Same constant used by the BRS page (keep identical for reproducible cross-checking)
  const GRAMS_TO_POUNDS = 0.00220462;

  function roundTo(value, decimals) {
    const d = Number(decimals);
    if (!Number.isFinite(value) || !Number.isFinite(d) || d < 0) return NaN;
    const factor = 10 ** d;
    // EPSILON reduces edge rounding issues like 1.005 -> 1.00
    return Math.round((value + Number.EPSILON) * factor) / factor;
  }

  function requireFiniteNumber(name, v) {
    const n = Number(v);
    if (!Number.isFinite(n)) throw new Error(`${name} must be a valid number.`);
    return n;
  }

  function calculateSaltMix(params) {
    if (!params || typeof params !== "object") {
      throw new Error("Params object is required.");
    }

    const productName = String(params.productName ?? "");
    if (!Object.prototype.hasOwnProperty.call(SALT_MIX_PRODUCTS, productName)) {
      throw new Error(
        `Unknown product "${productName}". Use one of: ${Object.keys(SALT_MIX_PRODUCTS).join(", ")}`
      );
    }

    const volumeGallons = requireFiniteNumber("Water volume (US gallons)", params.volumeGallons);
    const currentPpt = requireFiniteNumber("Current salinity (PPT)", params.currentPpt);
    const desiredPpt = requireFiniteNumber("Desired salinity (PPT)", params.desiredPpt);

    // Temperature is accepted (for your UI), but BRS formula does not use it when inputs are PPT.
    // We validate only if provided.
    if (params.temperatureC !== undefined && params.temperatureC !== null && params.temperatureC !== "") {
      requireFiniteNumber("Temperature (°C)", params.temperatureC);
    }

    if (volumeGallons <= 0) throw new Error("Water volume must be > 0.");
    if (currentPpt < 0) throw new Error("Current salinity must be ≥ 0.");
    if (desiredPpt < 0) throw new Error("Desired salinity must be ≥ 0.");
    if (!(desiredPpt > currentPpt)) throw new Error("Desired salinity must be greater than current salinity.");

    const factor = SALT_MIX_PRODUCTS[productName]; // grams / (gallon * PPT)
    const deltaPpt = desiredPpt - currentPpt;

    // Core mass (grams) inferred from BRS lbs-formula:
    const gramsRaw = deltaPpt * volumeGallons * factor;
    const kilogramsRaw = gramsRaw / 1000;
    const poundsRaw = gramsRaw * GRAMS_TO_POUNDS; // matches BRS display logic

    // 0.001 precision outputs
    const grams = roundTo(gramsRaw, 3);
    const kilograms = roundTo(kilogramsRaw, 3);
    const pounds = roundTo(poundsRaw, 3);

    return {
      productName,
      factor_grams_per_gal_per_ppt: factor,
      volumeGallons: roundTo(volumeGallons, 6),
      currentPpt: roundTo(currentPpt, 6),
      desiredPpt: roundTo(desiredPpt, 6),
      deltaPpt: roundTo(deltaPpt, 6),
      grams,
      kilograms,
      pounds, // optional cross-check vs BRS
      formatted: {
        grams: grams.toFixed(3),
        kilograms: kilograms.toFixed(3),
        pounds: pounds.toFixed(3)
      }
    };
  }

  // Optional helper to wire into a simple HTML UI
  // Requires elements: selectedProduct, waterVolume, currentSalinity, desiredSalinity, temperatureC(optional), resultOutput
  function wireToDom(config) {
    const cfg = config || {};
    const selectId = cfg.selectId || "selectedProduct";
    const volumeId = cfg.volumeId || "waterVolume";
    const currentId = cfg.currentId || "currentSalinity";
    const desiredId = cfg.desiredId || "desiredSalinity";
    const temperatureId = cfg.temperatureId || "temperatureC";
    const outputId = cfg.outputId || "resultOutput";

    const sel = document.getElementById(selectId);
    const volEl = document.getElementById(volumeId);
    const curEl = document.getElementById(currentId);
    const desEl = document.getElementById(desiredId);
    const tempEl = document.getElementById(temperatureId);
    const outEl = document.getElementById(outputId);

    if (!sel || !volEl || !curEl || !desEl || !outEl) {
      throw new Error("Missing required DOM elements to wire calculator.");
    }

    // Populate dropdown from extracted list (ensures ALL products are available)
    sel.innerHTML = "";
    for (const [name, factor] of Object.entries(SALT_MIX_PRODUCTS)) {
      const opt = document.createElement("option");
      opt.value = String(factor);
      opt.textContent = name;
      sel.appendChild(opt);
    }

    function recalc() {
      try {
        const productName = sel.options[sel.selectedIndex]?.textContent || "";
        const res = calculateSaltMix({
          productName,
          volumeGallons: volEl.value,
          currentPpt: curEl.value,
          desiredPpt: desEl.value,
          temperatureC: tempEl ? tempEl.value : undefined
        });

        outEl.textContent =
          `Salt needed: ${res.formatted.kilograms} kg (${res.formatted.grams} g)` +
          `  [BRS-check: ${res.formatted.pounds} lb]`;
      } catch (e) {
        outEl.textContent = (e && e.message) ? e.message : String(e);
      }
    }

    ["input", "change"].forEach((evt) => {
      sel.addEventListener(evt, recalc);
      volEl.addEventListener(evt, recalc);
      curEl.addEventListener(evt, recalc);
      desEl.addEventListener(evt, recalc);
      if (tempEl) tempEl.addEventListener(evt, recalc);
    });

    recalc();
  }

  return {
    SALT_MIX_PRODUCTS,
    calculateSaltMix,
    wireToDom
  };
});
