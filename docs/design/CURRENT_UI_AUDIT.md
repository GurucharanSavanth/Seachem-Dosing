# Current UI Audit — Master Issue Registry (v2.0-wip)

Commit `1d0744f` audit, refreshed 2026-06-29 for History wiring and AI removal.
Architecture / state / consistency issues. Accessibility issues → `ACCESSIBILITY_AUDIT.md`;
responsive → `RESPONSIVE_LAYOUT_AUDIT.md`; wiring → `UI_DOMAIN_CONNECTION_MATRIX.md`;
tokens/components → `DESIGN_SYSTEM_AUDIT.md`.

**"Claude Design produced/validated" column:** Claude Design is **not available** in this environment (no Claude Design plugin; design surfaces = Figma MCP + Mermaid). Every finding below is **code-grounded manual analysis**, not tool-generated. Marked `manual` throughout; honest by policy.

Severity: 🔴 high · 🟠 med · 🟡 low.

---

### ISSUE-ARCH-001 🟠 — Shell is still the legacy View stack
- **Component:** App shell · `MainActivity.kt:33,49,53`, `activity_main.xml`, `mobile_navigation.xml`
- **Current:** `AppCompatActivity` + ViewBinding + XML `BottomNavigationView` + XML nav graph + AppCompat top bar; Compose only inside per-fragment `ComposeView`.
- **Expected:** Either (a) keep the Fragment shell deliberately (documented), or (b) finish migration to a single-Activity Compose `NavHost` + `NavigationSuiteScaffold`. The half-state carries both nav stacks' cost.
- **Standard:** Android recommended app architecture; Compose navigation guidance.
- **User impact:** none directly. **A11y impact:** none. **Functional impact:** two theme worlds (see ISSUE-DS-008); harder maintenance.
- **Correction:** Decide in ADR. The unused `navigation-compose` dependency has been removed; migrate shell navigation only after the design-system layer lands.
- **Dependencies:** ADR-001 (Compose), design-system module.
- **Acceptance:** ADR recorded; no unused nav dependency on classpath; one navigation mechanism documented.
- **Tests:** build + `dependency-analysis` (unused-dep) check.
- **Claude Design:** manual.

### ISSUE-ARCH-002 🔴 — God ViewModel
- **Component:** `MainViewModel.kt` (628 lines, single `ViewModel` for all screens, `activityViewModels`)
- **Current:** One LiveData-backed VM owns profile, volume, ~20 water params, calculator
  scratchpad map, salt-mix, substrate, export, and a 26-arm dosing `when` (`:185-260`).
  Shared across legacy-profile/dashboard/calculator/settings paths; History now has a
  separate `HistoryViewModel`.
- **Expected:** Per-feature ViewModels (dashboard/calculator/medication/fertilizer/settings) over shared domain use cases; UDF with immutable state.
- **Standard:** Android app architecture (UDF, ViewModel-per-screen), SOLID (SRP).
- **User impact:** indirect (regression risk). **A11y:** none. **Functional:** high coupling; any screen can mutate global state; hard to test in isolation.
- **Correction:** Split incrementally behind the design-system work; route calculations through the **already-built** use cases (closes ISSUE-CONN-002).
- **Dependencies:** ISSUE-CONN-002, ISSUE-CALC (see below).
- **Acceptance:** each feature has its own VM; `MainViewModel` shrinks to shared app/profile state or is removed; unit tests per VM.
- **Tests:** new VM unit tests with coroutines-test; add a Flow probe dependency only if a test needs it.
- **Claude Design:** manual.

### ISSUE-ARCH-003 🟠 — Two contradictory state patterns
- **Component:** Medication/Fertilizer screens vs Dashboard/Calculators
- **Current:** Medication/Fertilizer keep state in `remember{}` and call domain engines inside the composable (`MedicationScreen.kt:91-130,159-197`); Dashboard/Calculators use the LiveData god-VM. No consistent pattern.
- **Expected:** One state-holder pattern app-wide (ViewModel + UDF).
- **Standard:** Android architecture; "no business logic in composables."
- **User impact:** inconsistent behaviour across tabs. **A11y:** none. **Functional:** engine calls recompute on every recomposition; not testable without UI.
- **Correction:** Introduce `MedicationViewModel` / `FertilizerViewModel`; move engine calls out of composables.
- **Acceptance:** no `domain.*` call inside a `@Composable`; both screens driven by VM state.
- **Tests:** VM unit tests; Compose UI test asserts state survives recomposition.
- **Claude Design:** manual.

### ISSUE-STATE-001 🔴 — All screen-local state is volatile
- **Component:** every Compose screen · grep: **zero `rememberSaveable`**
- **Current:** `remember{ mutableStateOf(...) }` for water type, expert toggle, symptom chips, dropdown selections, entered volume, assessment result (`MedicationScreen.kt:66,92-99,160-164`). Lost on rotation / process death. (God-VM params survive via `SavedStateHandle`; screen interaction state does not.)
- **Expected:** `rememberSaveable` (or VM-owned state) for any input the user can lose.
- **Standard:** Android state restoration; Compose state guidance.
- **User impact:** 🔴 user fills a medication/fertilizer flow, rotates or backgrounds, everything resets. **A11y:** worse for users who navigate slowly. **Functional:** flow restart, possible silent data loss.
- **Correction:** `rememberSaveable` for primitives/selections; lift complex flow state into the new feature VMs (ties to ISSUE-ARCH-003).
- **Acceptance:** rotate + process-death at each screen preserves inputs.
- **Tests:** instrumented `recreate()` / process-death restoration tests (T-STATE-1..n).
- **Claude Design:** manual.

### ISSUE-ARCH-004 🟡 — Dead VM code
- **Component:** `MainViewModel.kt:145-156,605+` (legacy holders/setters)
- **Current:** "Legacy" holders/setters "kept for compatibility if referenced"; fragments that referenced them are gone. AI/chat resources were removed by ADR-010.
- **Expected:** removed after non-reference confirmed.
- **Standard:** dead-code hygiene; YAGNI.
- **User impact:** none. **Functional:** noise, false "feature exists" signal.
- **Correction:** confirm via grep + lint, then delete remaining dead VM code in a dedicated cleanup commit.
- **Acceptance:** classification = obsolete+unreferenced; build green after removal.
- **Tests:** build + lint.
- **Claude Design:** manual.

### ISSUE-I18N-001 🟠 — New modules are English-only
- **Component:** Medication + Fertilizer screens · `MedicationScreen.kt:54-62,68,72-73,…`
- **Current:** UI strings are hardcoded literals; code self-flags it: `// ponytail: English literals … extract to strings.xml + values-kn when localizing (debt noted)` (`MedicationScreen.kt:54`). Rest of app is EN + KN. Two nav labels also hardcoded (`mobile_navigation.xml:26,31`).
- **Expected:** all user strings in `values/strings.xml` (+ `values-kn`).
- **Standard:** Android localization; WCAG 3.1.1 (language).
- **User impact:** Kannada users get mixed-language UI. **A11y:** screen readers announce wrong-language text. **Functional:** parity break with shipped localization.
- **Correction:** extract to `strings.xml`, add `values-kn`, replace literals; localize nav labels.
- **Acceptance:** no hardcoded user-facing literal in medication/fertilizer; lint `HardcodedText` clean; KN strings present.
- **Tests:** lint; locale-switch UI smoke.
- **Claude Design:** manual.

---

## Summary counts
- 🔴 high: ISSUE-STATE-001, ISSUE-ARCH-002, plus ISSUE-CONN-002/003 and ISSUE-DS-001/007 (own docs), ISSUE-A11Y-001 (own doc).
- 🟠 med: ARCH-001, ARCH-003, I18N-001, DS-003/004/005/006/008, CONN-001/004, RESP-001.
- 🟡 low: ARCH-004, NAV-001, DS-002.

Sequencing and per-screen retain/refactor classification: `TARGETED_UI_REMEDIATION_PLAN.md`.
