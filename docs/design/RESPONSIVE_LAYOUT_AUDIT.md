# Responsive / Adaptive Layout Audit — v2.0-wip

Standard: Material 3 adaptive layouts + Android window-size classes. Commit `1d0744f`. No code changed. Grep-confirmed: **no `WindowSizeClass` / `NavigationRail` / `calculateWindowSizeClass`** anywhere.

### ISSUE-RESP-001 🟠 — Phone-only layout; no window-size adaptation
- **Component:** whole app shell + all screens
- **Current:**
  - Navigation is `BottomNavigationView` only (`MainActivity` / `activity_main.xml`); no `NavigationRail`/drawer for medium/expanded widths.
  - Every screen is a single `Column(Modifier.fillMaxSize().verticalScroll(...))` (`MedicationScreen.kt:67`) → on tablets/landscape/foldable-unfolded the content is one stretched column with large empty side gutters.
  - No `WindowSizeClass`, no canonical adaptive patterns (list-detail, supporting-pane).
  - No foldable posture handling (no hinge/`WindowInfoTracker`).
- **Expected:** Compact = bottom bar; Medium = navigation rail; Expanded = rail/drawer + multi-pane (e.g. catalog list-detail). Constrain content `max-width` and center on wide screens.
- **Standard:** M3 adaptive; Android large-screen quality guidelines.
- **User impact:** poor tablet/foldable/desktop-window experience (the app *targets* SDK 36 where large screens + resizable windows matter). **A11y:** none. **Functional:** none (it works, just unoptimised).
- **Correction:** introduce `WindowSizeClass` at the shell; if migrating shell to Compose, use `NavigationSuiteScaffold`; add a content `widthIn(max=...)` wrapper in the design system; consider list-detail for catalog/medication.
- **Dependencies:** shell decision (ISSUE-ARCH-001), design-system module.
- **Acceptance:** rail appears ≥ medium width; content not full-bleed-stretched on expanded; foldable tabletop/`HALF_OPENED` does not break layout.
- **Tests:** Compose tests at compact/medium/expanded; resizable-emulator + foldable emulator manual pass; screenshot tests per size class.
- **Claude Design:** manual (proposal would benefit from Figma adaptive mockups — see remediation).

### Edge-to-edge / insets (acceptable, note)
- Edge-to-edge enabled in the **View** layer: `WindowCompat.setDecorFitsSystemWindows(window,false)` + inset listener padding the app bar / nav / nav-host (`MainActivity.kt:31,40-47,138-143`). Compose content receives an already-padded host.
- Works, but is non-idiomatic for Compose (content can't draw under bars; insets owned by Views). If/when the shell migrates to Compose, move to `Scaffold` + `WindowInsets`. Low priority.

### Orientation
- Vertical scroll Columns survive landscape without clipping (good). No landscape-specific layout (acceptable for compact; revisit with RESP-001).

**Net:** functional on phones; not adaptive. Single highest-leverage fix = window-size-class-aware navigation + max-width content wrapper, done once in the shell/design system.
