# Seachem Dosing — Web (v2.0)

Vite + TypeScript + PWA frontend per [ADR-004](../docs/architecture/adr-004-web-stack.md).

**Status:** alpha-bootstrap. Calculator implementations are being ported from `Base_Template/js/dosingCalculations.js` and `app/src/main/java/com/example/seachem_dosing/logic/SeachemCalculations.kt` per ADR-004 sub-decision **4a (full parity)** — 14 Android-only calculators must land here before this directory replaces `Base_Template/`.

## Setup

```bash
cd web
npm install
```

## Scripts

| Command | Purpose |
|---|---|
| `npm run dev` | Start Vite dev server (HMR). |
| `npm run build` | Production build → `dist/`. |
| `npm run preview` | Serve `dist/` locally for smoke-test. |
| `npm run lint` | Run ESLint (typescript-eslint v8 rules). |
| `npm run typecheck` | Run `tsc --noEmit` (no build, just type errors). |
| `npm test` | Run Vitest unit tests. |
| `npm run test:coverage` | Run tests with coverage (v8 provider). |
| `npm run sync-check` | Run `scripts/verify-sync.js` to validate calc parity with Kotlin. |

## Layout

```
web/
├── src/
│   ├── core/            # Constants + unit conversion + validators
│   ├── calculators/     # One file per calculator (KHCO3, Equilibrium, REEF_*, FLOURISH_*, etc.)
│   ├── models/          # TypeScript types mirroring Kotlin domain/model
│   ├── services/        # Storage, history (IndexedDB), i18n, AI client
│   └── ui/              # Components, theme, pages
├── tests/
└── public/              # Static assets, PWA icons
```

## Sync invariant

This codebase shares calculation coefficients with:
- `app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt`

Any change to a shared coefficient must update both files in the same commit.
CI runs `node scripts/verify-sync.js` as a hard gate.

## Migration status

- [x] Bootstrap (this commit) — `package.json`, `tsconfig.json`, `vite.config.ts`, ESLint
- [x] Port `utils.js` constants → `src/core/constants.ts`
- [ ] Port 9 existing calculators from `Base_Template/js/dosingCalculations.js`
- [ ] Port 14 Android-only calculators from `SeachemCalculations.kt` (per ADR-004 4a)
- [ ] PWA manifest + service worker
- [ ] IndexedDB history service
- [ ] i18n service (EN + KN)
- [ ] Replace `Base_Template/` (move to `archive/Base_Template-v1/`)
