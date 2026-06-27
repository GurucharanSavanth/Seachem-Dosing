# ADR-004: Web Stack — Vite + TypeScript + Full Calculator Parity

**Status:** Accepted
**Date:** 2026-05-09
**Deciders:** Gurucharan.S
**User decision:** B (Vite + TS) + sub-decision 4a (full parity with Android)

## Context

Current web app (`Base_Template/`):
- Single-page `index.html` with `<script>`-tag globals (no module system).
- 4 JS files: `app.js`, `dosingCalculations.js`, `uiHandlers.js`, `utils.js`, `translations.js`.
- 9 calculators: KHCO3, Equilibrium, NeutralRegulator, AcidBuffer, GoldBuffer, Safe, APT Complete, Prime, Stability.
- No build step. No tests in repo (README claims Jest/ESLint but `package.json` is missing).
- No PWA. No offline support. No type safety.

Android side (`SeachemCalculations.kt` + `Calculations.kt` + `SaltMixCalculations.kt`):
- 9 calculators matching web (in `Calculations.kt`, sync-honored).
- **14+ additional calculators in `SeachemCalculations.kt`** with NO web counterpart:
  - ALKALINE_BUFFER, POTASSIUM_BICARBONATE, NEUTRAL_REGULATOR (BigDecimal precision variants)
  - FLOURISH, FLOURISH_IRON, FLOURISH_NITROGEN, FLOURISH_PHOSPHORUS, FLOURISH_POTASSIUM, FLOURISH_TRACE
  - REEF_ADVANTAGE_CALCIUM, REEF_ADVANTAGE_MAGNESIUM, REEF_ADVANTAGE_STRONTIUM
  - REEF_BUFFER, REEF_BUILDER, REEF_CARBONATE
  - REEF_CALCIUM, REEF_COMPLETE, REEF_FUSION_1, REEF_FUSION_2, REEF_IODIDE, REEF_STRONTIUM
  - Substrate (Gravel) calculator with 11 product specs.
  - SaltMix calculator with 17 product factors (Kotlin already integrated; JS exists in `Feature_Update/salt-mix-calculator.js` orphan).

User decision: **4a — full parity**. Web v2.0 ships with all 23+ calculators that Android has.

## Decision

Adopt **Vite 5+ TypeScript strict-mode + ES6 modules + PWA** in a new `web/` directory (replacing `Base_Template/`). Port all 14+ Android-only calculators to web using identical formulas. Maintain calculation sync invariant.

## Options Considered

### Option A — Modularize vanilla JS only
Keep current approach: split into ES6 modules, add JSDoc, add `// @ts-check`, add Jest, add manifest+service-worker. No build step.

| Dimension | Score (1-5) |
|---|---|
| Maintenance burden | 3 |
| Type safety | 2 (JSDoc only) |
| Sharing with Android | 1 |
| Build complexity | 5 |

**Verdict:** Acceptable but doesn't unlock TS strict-mode benefits.

### Option B — Vite + TypeScript (CHOSEN)
TypeScript strict-mode. Vite for dev server + production bundling. Vitest for tests. PWA via `vite-plugin-pwa` (Workbox). ESLint with typescript-eslint.

| Dimension | Score (1-5) |
|---|---|
| Maintenance burden | 4 |
| Type safety | 5 |
| Sharing with Android | 2 (still divergent codebases) |
| Build complexity | 3 |

**Pros:** Strict types catch coefficient drift at compile time. Vite HMR <50ms. Vitest reuses Jest API. Workbox PWA mature.
**Cons:** Adds build step. CI needs Node. `package.json` becomes part of repo.

### Option C — Kotlin/Wasm shared codebase
Single Kotlin Multiplatform module compiles to Android + Wasm. Single source of truth for calculations.

| Dimension | Score (1-5) |
|---|---|
| Maintenance burden | 5 (long-term) |
| Type safety | 5 |
| Sharing with Android | 5 (literally same code) |
| Build complexity | 1 (KMP/Wasm still cutting-edge in 2026) |

**Verdict:** Eliminates sync invariant entirely. But Wasm output for math-only modules has tooling immaturity (debugger, source maps for Wasm). Defer to v3.0.

## Sub-decision: Parity Scope

User picked **4a** — full parity. Implications: 14+ new calculators on web side.

| Calculator | Source | Effort |
|---|---|---|
| ALKALINE_BUFFER (BigDecimal) | `SeachemCalculations.kt:102` | Port + unit tests |
| POTASSIUM_BICARBONATE (BigDecimal) | `SeachemCalculations.kt:165` | Port + unit tests (current `dosingCalculations.js:calculateKhco3Grams` is Double-based; need precision parity) |
| NEUTRAL_REGULATOR (BigDecimal) | `SeachemCalculations.kt:115` | Port + unit tests |
| ACID_BUFFER (BigDecimal) | `SeachemCalculations.kt:138` | Port + unit tests |
| EQUILIBRIUM (BigDecimal) | `SeachemCalculations.kt:153` | Port + unit tests |
| FLOURISH | `SeachemCalculations.kt:315` | Port |
| FLOURISH_IRON / N / P / K / TRACE | `SeachemCalculations.kt:322-371` | 5 ports + tests |
| REEF_ADVANTAGE_CALCIUM / Mg / Sr | `SeachemCalculations.kt:223-254` | 3 ports + tests |
| REEF_BUFFER / BUILDER / CARBONATE | `SeachemCalculations.kt:179-210` | 3 ports + tests |
| REEF_CALCIUM / COMPLETE / FUSION_1 / FUSION_2 / IODIDE / STRONTIUM | `SeachemCalculations.kt:256-310` | 6 ports + tests |
| Substrate (Gravel) | `SeachemCalculations.kt:374` | Port + 11 product specs |
| SaltMix | `Feature_Update/salt-mix-calculator.js` (already JS) | Move to `web/src/calculators/saltMix.ts`, port to TS |

For BigDecimal Kotlin functions, web port uses `decimal.js` library or `BigInt`-based fixed-point if precision-critical. Default: `Number` (IEEE 754) since web sync-check passes within 1e-9.

## Web Module Structure

```
web/
├── public/
│   └── manifest.json             # PWA manifest
├── src/
│   ├── core/
│   │   ├── constants.ts          # All COEFF_* + conversion factors (sync with Calculations.kt)
│   │   ├── unit-converter.ts
│   │   ├── input-validator.ts
│   │   └── calculator-engine.ts
│   ├── calculators/
│   │   ├── khco3.ts
│   │   ├── equilibrium.ts
│   │   ├── neutral-regulator.ts
│   │   ├── acid-buffer.ts
│   │   ├── gold-buffer.ts
│   │   ├── safe.ts
│   │   ├── apt-complete.ts
│   │   ├── prime.ts
│   │   ├── stability.ts
│   │   ├── alkaline-buffer.ts
│   │   ├── flourish.ts
│   │   ├── flourish-{iron,n,p,k,trace}.ts
│   │   ├── reef-advantage-{ca,mg,sr}.ts
│   │   ├── reef-{buffer,builder,carbonate,calcium,complete}.ts
│   │   ├── reef-fusion-{1,2}.ts
│   │   ├── reef-{iodide,strontium}.ts
│   │   ├── substrate.ts          # 11 product specs
│   │   └── salt-mix.ts           # 17 product factors
│   ├── models/
│   │   ├── product.ts            # Mirror Kotlin enum SeachemCalculations.Product
│   │   ├── profile.ts
│   │   ├── unit-scale.ts         # MEQ_L | DKH | PPM | PH
│   │   └── dosing-result.ts      # { value, unit, secondaryValue, secondaryUnit }
│   ├── services/
│   │   ├── storage-service.ts    # localStorage wrapper with schema versioning
│   │   ├── history-service.ts    # IndexedDB for parameter history (web parallel to Room)
│   │   └── i18n-service.ts       # EN/KN translations (extensible)
│   ├── ui/
│   │   ├── components/
│   │   ├── pages/
│   │   └── theme.ts
│   ├── app.ts                    # Bootstrap
│   └── main.ts                   # Entry
├── tests/
│   ├── unit/
│   │   └── calculators/*.test.ts
│   └── sync-validation.test.ts   # Compares against Kotlin via scripts/verify-sync.js output
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── vitest.config.ts
└── .eslintrc.cjs
```

## Stack Pinned Versions (proposed)

- Vite 5.x
- TypeScript 5.5+
- Vitest 1.x
- ESLint 9 + typescript-eslint 8
- vite-plugin-pwa (Workbox) 0.20+
- decimal.js 10.x (for BigDecimal-precision parity if needed)

No frontend framework (React/Vue) — keep zero-runtime-dep ethos. Use vanilla DOM + small reusable components, or `lit-html` for templating.

## Consequences

**Easier:**
- Adding a calculator → write `calculators/foo.ts` (pure function) + UI card mount. Type system enforces shape.
- TS strict mode catches coefficient drift, NaN paths, missing enum branches.
- PWA installable on mobile, works offline.
- IndexedDB history mirrors Android Room behavior.

**Harder:**
- Build step in dev loop (Vite HMR mitigates).
- CI must run `npm ci && npm run lint && npm test && npm run build`.
- Calculation sync invariant now spans 3 places: `Calculations.kt`, `dosingCalculations.js` (legacy until migration done), `web/src/calculators/*.ts`. Two-source migration period.

**Revisit if:**
- Bundle size grows >300KB (consider code-splitting per route).
- TypeScript strict-mode causes friction for casual contributors → relax to `noImplicitAny: true` only.

## Action Items

1. Create `web/` dir parallel to `Base_Template/`.
2. Initialize `package.json`, `tsconfig.json` (strict), `vite.config.ts`, `vitest.config.ts`, `.eslintrc.cjs`.
3. Port `utils.js` constants → `web/src/core/constants.ts`. Add JSDoc-derived comments referencing Seachem labels.
4. Port 9 existing `dosingCalculations.js` functions → `web/src/calculators/*.ts` (one file each).
5. Add 14 new calculators from `SeachemCalculations.kt` → `web/src/calculators/*.ts`.
6. Move `Feature_Update/salt-mix-calculator.js` → `web/src/calculators/salt-mix.ts` (port to TS).
7. Build PWA scaffold (`vite-plugin-pwa`, `manifest.json`, app icons from `mipmap`).
8. Add `web/tests/sync-validation.test.ts` that imports `web/src/core/constants.ts` and compares to a JSON dump of Kotlin constants (produced by `scripts/verify-sync.js --emit-json`).
9. Once `web/` is at parity, archive `Base_Template/` → `archive/Base_Template-v1/` (don't delete; useful reference for 1 release cycle).
10. Update README.md to point at `web/` and remove stale `npm test`/`npm run lint` claims.

## Rollback Criteria

Revert to Option A (vanilla JS modular) if:
- TS migration uncovers >5 calculator formula bugs that block shipping.
- Vite build time grows >30s for production build.
- PWA registration creates user-facing issues (cache staleness reports >2 in feedback).

## Revisit Path to Option C (KMP/Wasm)

In v3.0, evaluate Kotlin Multiplatform with a `commonMain` `:dosing-engine` module compiled to JVM (Android) + Wasm (web). Eliminates sync invariant. Requires Kotlin/Wasm tooling maturity assessment in 2027.
