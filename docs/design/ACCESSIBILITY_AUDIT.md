# Accessibility Audit — v2.0-wip

Standard: WCAG 2.2 AA + Android accessibility guidance. Commit `1d0744f`. No code changed.

**Coverage honesty:** fully read = `MedicationScreen`, `MainActivity`, theme. Sampled via grep = others. A complete pass still needs: (a) read of `Calculators/Fertilizer/Settings/Profile` screens, (b) **on-device TalkBack + Switch Access traversal**, (c) **numeric contrast measurement** of the palette. Items below are what the code proves or strongly indicates; contrast and traversal are marked *measure/verify*, not asserted.

Severity: 🔴 high · 🟠 med · 🟡 low.

### Positives (retain)
- Type uses `MaterialTheme.typography.*` everywhere read → **honours dynamic font scaling**. Screens are `verticalScroll` Columns (`MedicationScreen.kt:67`) → large fonts reflow, no clipping. Good.
- M3 components apply `minimumInteractiveComponentSize()` (48dp) by default → chip/switch **touch targets** are enforced even when visually 32dp, *provided enforcement is not disabled* (verify no `LocalMinimumInteractiveComponentEnforcement provides false`). Currently no such override found → likely compliant (downgraded from a suspected fail).

### ISSUE-A11Y-001 🟠 — No explicit semantics on custom interactive composables
- **Component:** `AppDropdown` variants, `WarningCard`, `AdviceCard`, status cards · e.g. `MedicationScreen.kt:82,135,221`
- **Current:** zero `Modifier.semantics{}`/`contentDescription` in read code; relies entirely on M3 default roles. Screen titles (`headlineMedium`) are not marked `heading()`.
- **Expected:** headings marked `semantics { heading() }`; safety panels exposed as headings or `liveRegion`; any icon-only control gets `contentDescription`.
- **Standard:** WCAG 1.3.1, 4.1.2; Android semantics.
- **User impact:** TalkBack users can't jump by heading; safety warning announced as ordinary inline text. **Functional:** none.
- **Correction:** add `heading()` to section titles; mark `WarningCard` content as a heading/liveRegion in the shared component.
- **Acceptance:** TalkBack heading navigation reaches each section; warning announced with emphasis.
- **Tests:** Compose `assertContentDescriptionEquals` / `onNode(hasHeading())`; manual TalkBack.
- **Claude Design:** manual.

### ISSUE-A11Y-002 🟠 — Safety status risks color-reinforced meaning (verify)
- **Component:** `BeginnerProductRow` / `AdviceCard` · `MedicationScreen.kt:201-213,135-153`
- **Current:** state shown via container color (`errorContainer` for Blocked) **plus** a text label ("Blocked: …", "Compatible — …"). Text is present → not color-*alone* (passes 1.4.1 as written). Risk: future edits dropping the text label.
- **Expected:** keep an explicit text/icon status token independent of color; lock with a test.
- **Standard:** WCAG 1.4.1 (use of color).
- **Correction:** introduce a `StatusBadge(text+icon)` in the design system so color is never the sole carrier.
- **Acceptance:** every safety state has non-color text/icon; regression test asserts label text.
- **Tests:** Compose UI test on each `CalcResult` branch.
- **Claude Design:** manual.

### ISSUE-A11Y-003 🟡 — Contrast unmeasured
- **Component:** palette `ui/theme/Color.kt`
- **Current:** hand-authored M3 roles; not numerically verified ≥4.5:1 (text) / 3:1 (large/UI). E.g. `onSurfaceVariant` greys on container colors used for secondary text (`MedicationScreen.kt:211`).
- **Expected:** all text/background pairs measured AA.
- **Standard:** WCAG 1.4.3 / 1.4.11.
- **Correction:** run a contrast check (Accessibility Scanner on device, or compute ratios) for each role pair; adjust failing tokens at the single source (ISSUE-DS-001).
- **Acceptance:** documented ratio table, all AA.
- **Tests:** Accessibility Scanner export; optional unit check on token pairs.
- **Claude Design:** manual.

### ISSUE-A11Y-004 🟠 — Wrong-language announcements (see ISSUE-I18N-001)
Hardcoded English in medication/fertilizer + nav labels → TalkBack announces English under a Kannada locale. Cross-ref `CURRENT_UI_AUDIT.md` ISSUE-I18N-001. Standard: WCAG 3.1.1/3.1.2.

### Deferred (needs the 4 unread screens + device)
- Focus order / traversal index on multi-column FlowRow chips (`MedicationScreen.kt:170`) — verify logical order under TalkBack.
- Switch Access reachability of the ExposedDropdown menu items.
- `Profile`/`Calculators`/`Fertilizer`/`Settings` semantics — not yet read.

**Next action:** on-device Accessibility Scanner + TalkBack pass after the design-system component layer exists (so fixes land once, in shared components, not 4×).
