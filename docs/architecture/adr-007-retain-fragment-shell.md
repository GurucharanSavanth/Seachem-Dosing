# ADR-007: Retain the Fragment/XML app shell (this phase)

**Status:** Accepted
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Supersedes:** none · **Related:** ADR-001 (Compose for screens), `docs/design/TARGETED_UI_REMEDIATION_PLAN.md`

## Context
The shell is the legacy View stack — `MainActivity` (`AppCompatActivity` + ViewBinding) hosting `activity_main.xml` → `NavHostFragment` + `BottomNavigationView` + AppCompat top app bar; Compose renders only leaf screens inside per-fragment `ComposeView` (e.g. `DashboardFragment.kt:41-51`). The unused `androidx.navigation:navigation-compose` dependency was removed after repo-wide proof. See `docs/design/SCREEN_INVENTORY.md`, `NAVIGATION_AND_FLOW_MAP.md`.

## Decision
Keep the Fragment/XML shell for the current remediation phase. Do **not** migrate to single-Activity Compose `NavHost` now. This is a *temporary* target, not declared permanent.

Rationale: the shell works; a nav migration carries broad route, lifecycle, back-stack, saved-state, deep-link, transition, accessibility, and regression risk with no immediate user-visible benefit. Remediation value (state restoration, domain wiring, design tokens, History) is independent of the shell and ships faster without a nav rewrite.

## Constraints recorded
- **Navigation ownership:** Fragment Navigation component (`mobile_navigation.xml`), `BottomNavigationView.setupWithNavController`. State shared via `MainViewModel` (`activityViewModels`).
- **Lifecycle / state restoration:** `ComposeView` uses `DisposeOnViewTreeLifecycleDestroyed`; screen-local Compose state must move to `rememberSaveable`/VM (ISSUE-STATE-001) regardless of shell.
- **Deep links:** none currently defined; no app-link verification. Out of scope this phase.
- **Back-stack:** single bottom-nav graph; Profile/Settings via top-bar overflow. Profile hides chrome (`MainActivity.kt:58-61`).
- **Adaptive-nav limitation:** `BottomNavigationView` only; no rail/drawer for medium/expanded widths (RESP-001). A Compose adaptive surface can be introduced later without a full shell rewrite.

## `navigation-compose` removal
Remove the unused dependency **only after** a repository-wide proof of non-use:
repo-wide symbol/dep search · `./gradlew :app:dependencies` / dependency-analysis · compile all variants · unit tests · lint · relevant instrumented/nav tests. Tracked as a separate follow-up commit (not bundled with unrelated work).

## Criteria that would justify a future Compose-Navigation migration
Adaptive multi-pane nav becomes a product priority; deep-link/typed-args needs grow; or the XML theme is retired (ADR-009 completion). Prerequisites before such a migration: complete route inventory, typed destination model, deep-link inventory, saved-state parity, process-death restoration tests, back-navigation matrix, screenshot + accessibility baselines, screen-by-screen plan, explicit rollback boundary. That migration must **not** be mixed into feature/cleanup work.

## Rollback
No code change in this ADR. If the shell later blocks a required capability, revisit via the criteria above.
