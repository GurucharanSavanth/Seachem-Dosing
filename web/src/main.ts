/**
 * Web entry point — Phase 5 bootstrap placeholder.
 *
 * Real UI lands as Phase 5 ports calculators from Base_Template/ +
 * SeachemCalculations.kt per ADR-004 (full parity).
 */

import { US_GAL_TO_L, COEFF_EQUILIBRIUM } from './core/constants.js';

const app = document.getElementById('app');
if (app) {
  app.innerHTML = `
    <main style="font-family: system-ui, sans-serif; padding: 2rem; max-width: 36rem; margin: 0 auto;">
      <h1>Seachem Dosing — v2.0 (alpha)</h1>
      <p>Web frontend bootstrap. See <code>web/README.md</code> for migration status.</p>
      <p>Sanity check: <code>US_GAL_TO_L = ${US_GAL_TO_L}</code>, <code>COEFF_EQUILIBRIUM ≈ ${COEFF_EQUILIBRIUM.toFixed(6)}</code>.</p>
      <p>Use the legacy app at <a href="../Base_Template/index.html">Base_Template/</a> until Phase 5 calculator ports complete.</p>
    </main>
  `;
}
