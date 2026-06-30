# ADR-009: Compose-first semantic colour tokens + XML compatibility bridge

**Status:** Accepted
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Related:** ADR-007 (shell retained → XML theme still live)

## Context
Colour tokens live in **two hand-synced sources**: `ui/theme/Color.kt` (Compose) and `res/values/colors.xml` + `values-night/themes.xml` (XML). `Color.kt:5` literally says "Mirrors res/values/colors.xml … Keep in sync." Silent drift risk. The Fragment/XML shell (ADR-007) still consumes XML colours, so XML cannot be deleted yet.

## Decision
**Compose ColorScheme (`Color.kt` + `Theme.kt`) is the canonical semantic token model.** Retain a *controlled, documented* XML compatibility bridge for the shell while it exists — XML colours map to the **same semantic roles**, never independently hand-edited hex.

## Rules
- Tokens are **semantic roles**, not raw colour names: `primary/onPrimary/primaryContainer/onPrimaryContainer`, `secondary*`, `tertiary*`, `surface/surfaceVariant/surfaceContainer/surfaceContainerHigh`, `onSurface/onSurfaceVariant`, `background`, `error/errorContainer`, plus domain safety/evidence roles **only where Material roles are insufficient** (e.g. warning/success if not mapped to error/tertiary).
- No duplicated arbitrary hex across sources. Prefer **generated** XML compatibility resources from the Compose source if the build approach is deterministic/maintainable; otherwise a **minimal, explicitly documented** XML bridge with a **parity test**.
- Separate light / dark / dynamic-colour behaviour explicitly.
- **WCAG 2.2 AA contrast** validated for normal text, large text, icons, controls, warnings, and disabled states (ISSUE-A11Y-003).
- **No meaning by colour alone** — medication severity / safety / success / failure always carry text or icon (ISSUE-A11Y-002).
- Remove obsolete XML colours **only after** reference analysis proves them unused.

## Tests
Token-parity test (Compose role ↔ XML bridge value) · screenshot/golden tests light/dark/dynamic · contrast assertions for role pairs.

## Consequences
- **Easier:** one source of truth; drift eliminated; clean path to drop XML when the shell migrates (ADR-007 criteria).
- **Harder:** must build/maintain the bridge + parity test while two render stacks coexist.

## Rollback
If generation adds disproportionate build complexity, fall back to the minimal documented manual bridge (still single-source-authored, parity-tested). Do not return to two independently-edited palettes.
