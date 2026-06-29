# Aquarium Dosing & Water‑Parameter Calculator

> Legacy static web app documentation. `Base_Template/` has no local `package.json`
> or Jest test suite in this repository; npm/Jest/CI claims below are historical.
> The active TypeScript scaffold lives in `web/`.

## [Click here for Calculator](https://gurucharansavanth.github.io/Seachem-Dosing-Calculator/)

[![Live Demo](https://img.shields.io/badge/demo-online-brightgreen.svg)](https://gurucharansavanth.github.io/Seachem-Dosing-Calculator/)

> **A fast, accessible calculator for precise Seachem and DIY supplement dosing — now with dark mode, bilingual UI (English ⇄ Kannada), and full CI automation.**

---

## Table of Contents

* [Features](#features)
* [Screenshots](#screenshots)
* [Getting Started](#getting-started)
* [Usage](#usage)
* [Project Structure](#project-structure)
* [Calculation Reference](#calculation-reference)
* [Accessibility & i18n](#accessibility--i18n)
* [Testing](#testing)
* [CI/CD](#cicd)
* [Contributing](#contributing)
* [Acknowledgements](#acknowledgements)

---

## Features

| Category          | Details                                                                                                                                                                   |
| ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Supplements**   | Potassium Bicarbonate (**KH Booster**), Seachem **Equilibrium** (GH), **Neutral Regulator** (pH ≈ 7), **Acid Buffer** (KH ↓ & pH ↓), **Gold Buffer** (Goldfish‐specific). |
| **Units**         | Litres, US Gallons, UK Gallons.                                                                                                                                           |
| **Live Feedback** | Debounced auto‑calculation ✨; copy‑to‑clipboard; CSV export.                                                                                                              |
| **Accessibility** | WCAG 2.2 AA colours, full keyboard nav, `aria‑live` results, English + ಕನ್ನಡ toggle.                                                                                      |
| **Theming**       | One‑click light/dark (prefers‐color‑scheme aware).                                                                                                                        |
| **Responsive**    | Mobile‑first layout; footer action‑bar for Android/iOS convenience.                                                                                                       |
| **Reliability**   | Legacy static implementation; current automated web work is under `web/`.                                                                                                  |



## Getting Started

### Quick Start (no install)

1. Visit the **\[live demo]** — or
2. Download the repo and double‑click **`index.html`**.

### Local Dev

```bash
# Clone & enter
git clone https://github.com/GurucharanSavanth/Seachem-Dosing-Calculator.git
cd Seachem-Dosing-Calculator

# No local npm workflow exists in Base_Template in this repository.
# Use ../web for the current TypeScript scaffold.
```

> A simple static file server (e.g. `npx serve`) is handy for live‑reload previewing.

---

## Usage

1. **Volume** – enter net tank volume & choose unit.
2. **Supplement panel** – expand, fill *Current* and *Target* values (plus purity for KHCO₃).
3. Doses update automatically or via the **Calculate Doses** button.
4. Copy 📋 or **Download CSV** for record‑keeping.

*See in‑app tooltips (`?`) for parameter guidance.*

---

## Project Structure

```text
├── index.html
├── css/
│   └── styles.css            # WCAG‑compliant theme & layout
├── js/
│   ├── app.js               # Bootstraps UI, theme, events
│   ├── utils.js             # Constants, helpers, conversions
│   ├── dosingCalculations.js# 💡 Pure math — the critical path
│   ├── uiHandlers.js        # DOM glue & CSV export
│   └── translations.js      # EN ↔︎ KN strings
```

---

## Calculation Reference

| Function                         | Core Formula (g per L)                           | Source                             |
| -------------------------------- | ------------------------------------------------ | ---------------------------------- |
| `calculateKhco3Grams`            | `(ΔdKH × 0.017848 × L) ÷ purity`                 | **Seachem / K‑HCO₃ stoichiometry** |
| `calculateEquilibriumGrams`      | `ΔdGH × 0.066667 × L`                            | Seachem Equilibrium label          |
| `calculateNeutralRegulatorGrams` | Adaptive 0.0625–0.125 g/L × KH‑factor × pH‑steps | Seachem guide                      |
| `calculateAcidBufferGrams`       | `ΔdKH × 0.046875 × L` (**fixed v4.2**)           | Label: 1.5 g/40 L per 0.8 dKH      |
| `calculateGoldBufferGrams`       | `0.15 × L` (full dose ≥ 0.3 pH)                  | Seachem Gold Buffer                |

All equations are unit‑tested against manufacturer tables and stoichiometric checks (see `tests/`).

---

## Accessibility & i18n

* **Colour** — primary palette darkened to meet 4.5:1 contrast on white (#0059B3).
* **ARIA** — live regions, labelled controls, focus order preserved.
* **Keyboard** — fully navigable; copy buttons are standard `<button>`.
* **Languages** — toggle switches entire UI strings via `translations.js` (English ↔ Kannada).

---

## Testing

No Base_Template-local Jest/ESLint workflow exists in this repository. Current web
automation belongs to `web/` and repository CI.

---

## CI/CD

| Stage      | Tool         | Purpose                                              |
| ---------- | ------------ | ---------------------------------------------------- |
| **Lint/Test** | `web/` workflow | Current TypeScript scaffold checks                  |
| **Deploy** | GitHub Pages | Auto‑publish `main` → live demo (via Pages settings) |


---

## Contributing

1. **Fork** & create a feature branch (`git checkout -b feat/my-fix`).
2. For legacy static-only changes, verify the affected page manually; for the
   current TypeScript scaffold, use the workflows documented in `../web/README.md`.
3. Commit with [Conventional Commits](https://www.conventionalcommits.org/) (`chore:`, `fix:`, `feat:`…).
4. Open a PR – GitHub Actions will run automatically.

*Need an idea?* See [open issues](../../issues) or suggest improvements!

---


## Acknowledgements

* **Seachem Laboratories, Inc.** for publicly documenting dosage guidelines.
* [WCAG 2.2](https://www.w3.org/TR/WCAG22/) for accessibility standards.
* [twbs/colors](https://github.com/twbs/colors) inspiration for contrast‑safe palette.

---

> *Not affiliated with Seachem — this is a community tool. Use at your own risk and always double‑check critical doses.*
