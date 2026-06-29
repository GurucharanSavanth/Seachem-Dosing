# Seachem Dosing - Web (v2.0 alpha scaffold)

Vite + TypeScript scaffold per [ADR-004](../docs/architecture/adr-004-web-stack.md).
PWA configuration exists, but the app is not yet a verified installable/offline PWA
because required assets and smoke checks are incomplete.

**Status:** alpha scaffold. Some engine ports exist under `web/src`, but the user-facing
web UI still points users to the legacy `Base_Template/` app until UI/export/history/sync
parity is complete.

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
| `npm run sync-check` | Run the legacy `scripts/verify-sync.js` check for `Calculations.kt` ↔ `Base_Template`; it does not validate `web/src/**` yet. |

## Layout

```
web/
├── src/
│   ├── core/            # Constants + unit conversion + validators
│   ├── calculators/     # Legacy calculator ports
│   ├── core/            # Constants + unit conversion
│   └── engine/          # v2 engine dispatch and shared types
├── tests/
```

## Sync invariant

This codebase shares calculation coefficients with:
- `app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt`

Any change to a shared legacy coefficient must update both files in the same commit.
CI runs `node scripts/verify-sync.js` as a hard gate for that legacy pair only.
Full `web/src/**` parity still needs golden vectors against the Kotlin engines.

## Migration status

- [x] Bootstrap (this commit) — `package.json`, `tsconfig.json`, `vite.config.ts`, ESLint
- [x] Port `utils.js` constants → `src/core/constants.ts`
- [~] Port calculators/engines from `Base_Template` and Android (partial; UI parity pending)
- [ ] Add golden Kotlin parity tests for `web/src/**`
- [ ] Complete and verify PWA manifest, icons, service worker, install, and offline behavior
- [ ] IndexedDB history service
- [ ] i18n service (EN + KN)
- [ ] Replace `Base_Template/` (move to `archive/Base_Template-v1/`)
