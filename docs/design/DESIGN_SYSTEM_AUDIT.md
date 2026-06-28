# Design-System Audit — v2.0-wip

Grounded in `ui/theme/{Theme,Color,Type}.kt`, `res/values/{colors,themes,dimens}.xml`, `res/values-night/themes.xml`, and the 6 screen files. No code changed.

## Token inventory

| Token group | Source | State | Finding |
|---|---|---|---|
| Color | **Two sources**: `ui/theme/Color.kt` (Compose) *and* `res/values/colors.xml` + `values-night/themes.xml` (XML). `Color.kt:5` comment: "Mirrors res/values/colors.xml … Keep in sync." | Hand-synced duplication | ISSUE-DS-001 (high): single source needed; drift is silent. |
| Color scheme | `lightColorScheme`/`darkColorScheme` fully specified, all M3 roles mapped (`Theme.kt:9-41`) | Good | Keep. |
| Dynamic color | Not used — `SeachemTheme` ignores `dynamicLightColorScheme` (Android 12+) | Absent | ISSUE-DS-002 (low/intentional): brand-fixed palette; document the choice. |
| Typography | `Type.kt:7` = `Typography()` **default**, no custom scale | Stub | ISSUE-DS-003 (med): no brand type scale; all screens use raw M3 defaults. |
| Shape | None as tokens; `RoundedCornerShape(14.dp/16.dp)` hardcoded per card (`MedicationScreen.kt:83,146,208`) | Hardcoded | ISSUE-DS-004 (med). |
| Spacing | None in Compose; `.dp` literals inline (`16.dp`,`12.dp`,`8.dp`,`4.dp`). `res/values/dimens.xml` exists for the dead XML layer | Hardcoded | ISSUE-DS-005 (med). |
| Elevation | Default `CardDefaults` only | n/a | Acceptable. |
| Motion | `MaterialFadeThrough` fragment transitions (`DashboardFragment.kt:32-34`); no Compose motion tokens, no reduced-motion handling | Partial | ISSUE-DS-006 (med): no `Animatable`/reduced-motion story. |
| Profile accents | `Color.kt:66-68` (`ProfileFreshwater/Saltwater/Pond`) | Present, Compose-only | OK. |

## Component inventory (reusable vs duplicated)

Compose has **no `:core:designsystem` module** and **no shared component file**. Reusable widgets are re-declared privately in each screen:

| Component | Defined in (duplicated) | Note |
|---|---|---|
| `Dropdown` / `LabeledDropdown` / `UnitDropdown` (ExposedDropdownMenuBox wrapper) | `CalculatorsScreen:204`, `DashboardScreen:175`, `FertilizerScreen:165`, `MedicationScreen:221` | **4 near-identical copies** → ISSUE-DS-007 (high): extract one `AppDropdown`. (The 4 `menuAnchor` fixes in commit `1d0744f` had to be applied 4× precisely because of this duplication.) |
| `PrimaryButton` | `MedicationScreen:243` (and likely others) | duplicated full-width button wrapper |
| `WarningCard` | `MedicationScreen:82` | safety panel — should be a shared, semantically-labelled component (see ACCESSIBILITY). |
| `SwitchRow` | `MedicationScreen:235` | label+switch row |
| `AdviceCard` / result card | `MedicationScreen:135` | renders `CalcResult` sealed type — reusable across medication+fertilizer+dosing if extracted |
| Status/parameter card | `DashboardScreen` | dashboard recommendation/status |

**Consolidation target:** one design-system layer owning color (single source), type scale, spacing/shape/motion tokens, and the shared `AppDropdown / AppButton / WarningPanel / ResultCard / SwitchRow / SectionHeader`. See `TARGETED_UI_REMEDIATION_PLAN.md` step 1.

## Theme propagation (verify-on-device)
- All screens call `SeachemTheme { … }` with the default `darkTheme = isSystemInDarkTheme()` (`Theme.kt:46`; no call site overrides it — grep-confirmed).
- The toolbar menu switches theme via `AppCompatDelegate.setDefaultNightMode(...)` (`MainActivity.kt:88-98`) — a **View-layer** mechanism.
- **Suspected inconsistency (ISSUE-DS-008, med):** when the user forces Light/Dark against the system setting, the AppCompat-themed shell flips; whether the Compose content follows depends on AppCompat's config-override reaching `isSystemInDarkTheme()`. AppCompat *does* override the configuration uiMode, so it may work — **must be verified on device** (test T-THEME-1). If it does not follow, route the chosen mode into `SeachemTheme(darkTheme = …)` explicitly. Not asserted as a confirmed defect.
