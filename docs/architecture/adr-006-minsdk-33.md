# ADR-006: minSdk 33 (Android 13+)

**Status:** Accepted
**Date:** 2026-06-27
**Deciders:** Gurucharan.S
**User decision:** "MinSDK starting from 33 and up till latest — crucial for the features the latest SDKs offer."

## Context

The app shipped at `minSdk = 24` (Android 7.0). `compileSdk`/`targetSdk` were already modernized to 36 by the in-progress v2.0 work. The owner asked to "bump latest min Android support to SDK 34"; on clarification this meant the **minSdk floor** (compile/target were already past it), and the chosen floor is **33**.

Evidence considered:
- `POST_NOTIFICATIONS` is a runtime permission from **API 33** — a uniform minSdk 33 removes the dual-path notification permission handling for any future reminder/alert feature.
- Edge-to-edge is enforced at targetSdk 35+ (already targeting 36) — orthogonal to minSdk but reinforces the modern-baseline direction.
- All toolchain libraries (Compose BOM 2024.12.01, Koin 4.0.0, Room 2.6.1, lifecycle 2.8.7, Material 1.11.0) declare floors well below 33 — none blocks the bump.

## Decision

Set `minSdk = 33` (Android 13). Keep `targetSdk = 36`, `compileSdk = 36.1`.

## Options Considered

| Option | Coverage | Note |
|---|---|---|
| Keep minSdk 24 | ~99%+ | broadest; carries legacy-API burden |
| **minSdk 33 (CHOSEN)** | narrower than legacy floors | modern baseline; owner-selected |
| minSdk 34 | narrower than 33 | not chosen |

## Trade-off Analysis

minSdk 33 trades pre-Android-13 devices for a cleaner modern API floor. This is a deliberate, owner-approved product decision favouring latest-SDK capability over maximal device reach. It is reversible.

## Consequences

- **Easier:** no `Build.VERSION.SDK_INT < 33` guards (a scan found none in `app/src`); runtime-notification model is uniform; no legacy-API shims.
- **Harder:** the app will not install on pre-Android-13 devices — a visible reduction in addressable devices.
- **Verification:** `./gradlew assembleDebug testDebugUnitTest` must stay green after the bump (no API used below 33 — raising the floor cannot break compilation).

## Rollback Criteria

Lower `minSdk` again if post-launch analytics show the coverage loss is unacceptable. No code rewrite is required to roll back (the bump removed no APIs).
