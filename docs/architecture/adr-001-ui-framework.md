# ADR-001: UI Framework — Jetpack Compose

**Status:** Accepted
**Date:** 2026-05-09
**Deciders:** Gurucharan.S
**User decision:** B (Compose)

## Context

The Android app currently uses XML layouts + `MutableLiveData` + Fragment-based navigation (`androidx.navigation:navigation-fragment-ktx 2.7.7`). 12+ calculator cards are inflated as XML and configured imperatively in `CalculatorsFragment.kt:205-326` (`setupUniversalCard`) via `findViewById` + manual `TextWatcher` wiring.

Forces:
- 12 hand-written `card_*.xml` layouts (Acid, APT, Equilibrium, Gold, KHCO3, Neutral, Prime, Stability, Safe, WaterChange, Substrate, SaltMix, UniversalCalc) duplicate similar structures (header, expandable content, current/target inputs, result text).
- Imperative card setup wires every view by ID — adding a new calculator means: layout file + setup function + binding ID names + `applyProfileUi` registration.
- StateFlow migration (per ADR-002 stack direction) fits Compose's `collectAsStateWithLifecycle()` more naturally than XML observers.
- Material 3 components are first-class in Compose; XML M3 lags features.
- Project budget allows multi-week refactor; v2.0 timeline accepts UI churn.

## Decision

Adopt **Jetpack Compose** as the UI framework. Migrate fragment-by-fragment; XML remains during transition (dual-stack). New screens are Compose-only.

## Options Considered

### Option A — Modernize XML only
Keep XML, modernize accessibility, theme attributes, ViewBinding null-safety. Replace `findViewById` with ViewBinding everywhere.

| Dimension | Score (1-5) |
|---|---|
| Migration cost | 5 (cheapest) |
| Future-proofing | 1 |
| Test impact | 5 |
| Learning curve | 5 |

**Pros:** Zero risk. Existing XML stays.
**Cons:** Caps long-term UI velocity. Doesn't solve imperative card-wiring bloat.

### Option B — Migrate to Compose (CHOSEN)
Replace fragments + XML with Compose screens. Use `androidx.navigation:navigation-compose`. State via `StateFlow.collectAsStateWithLifecycle()`. Reusable composables for card patterns.

| Dimension | Score (1-5) |
|---|---|
| Migration cost | 2 (4-6 weeks for full app) |
| Future-proofing | 5 |
| Test impact | 3 (Compose test rule replaces Espresso) |
| Learning curve | 3 |

**Pros:** Composable card abstractions cut LOC. State binds naturally to StateFlow. Material 3 first-class. Easier theming.
**Cons:** Build time +15-20% (Compose compiler). APK +~1MB (Compose runtime).

### Option C — Hybrid (new Compose, existing XML)
Compose only for net-new screens (AI chat, parameter history charts). Keep dashboard/calculators/profile/settings in XML.

**Pros:** Lowest delivery risk.
**Cons:** Permanent dual-stack overhead. Two UI paradigms. Compose-in-Fragment glue gets ugly.

## Trade-off Analysis

Option A safe but caps maintainability. Option C creates permanent dual-stack overhead. Option B is highest one-time cost but pays back in calculator-card velocity (the dominant change pattern in this app) and aligns with ADR-002 (Koin) + ADR-003 (Room + Flow) data-flow.

The 12 `card_*.xml` files compress into ~3 reusable composables (`UniversalCalculatorCard`, `QuickDoseCard`, `SubstrateCard`) — measurable LOC reduction.

## Consequences

**Easier:**
- New calculator → write Composable + register in NavHost. No XML, no findViewById, no TextWatcher.
- Theming via MaterialTheme tokens propagates automatically.
- Recomposition automatic with StateFlow.

**Harder:**
- ViewBinding code (every fragment) needs rewrite.
- Animations require new APIs (`AnimatedVisibility`, `Modifier.animateContentSize`).
- Material XML components (PopupMenu, AlertDialog, AutoCompleteTextView) need composable replacements (DropdownMenu, Material3 AlertDialog, ExposedDropdownMenuBox).

**Revisit if:**
- Compose compiler bumps build time >30% on CI → evaluate KSP for compose-compiler.
- Compose-Navigation back-stack handling proves brittle → fallback to nav-fragment per-screen.

## Action Items

1. Add Compose BOM + lifecycle-runtime-compose + navigation-compose + activity-compose to `gradle/libs.versions.toml`.
2. Add `kotlin-compose` plugin (`org.jetbrains.kotlin.plugin.compose`).
3. Set `buildFeatures { compose = true }` in `app/build.gradle.kts`.
4. Create `ui/theme/` with `Color.kt`, `Theme.kt`, `Type.kt` matching existing `colors.xml` + `themes.xml` palette.
5. Write `UniversalCalculatorCard` composable + 1 reference screen (Calculators) before broader rollout.
6. Migrate ProfileSelection → Dashboard → Calculators → Settings in that order (smallest screens first).
7. Keep XML layouts working until each screen fully ported, then delete in same commit.

## Rollback Criteria

Revert to Option A if:
- Compose compiler bumps build time >30% on CI.
- More than 3 user-reported visual regressions vs XML version after migration completes.
- Critical Material 3 component blocks delivery (no XML escape hatch found).
