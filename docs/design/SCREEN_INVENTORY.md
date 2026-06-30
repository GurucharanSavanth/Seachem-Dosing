# Screen Inventory — v2.0-wip

Grounded in code at commit `1d0744f`; refreshed on 2026-06-29 for the current History
destination and AI-removal state.

## Shell pattern (applies to every screen)

The app is **not** full Compose. The shell is the legacy View stack; Compose renders only leaf content:

- `MainActivity` (`AppCompatActivity` + ViewBinding) hosts `activity_main.xml` → `NavHostFragment` + `BottomNavigationView` + AppCompat top app bar. `MainActivity.kt:33,49,53`
- Navigation = Fragment XML graph `res/navigation/mobile_navigation.xml` (not Navigation Compose).
- Each destination is a `Fragment` whose `onCreateView` returns a `ComposeView` calling `SeachemTheme { XxxScreen(...) }`. Pattern confirmed in `DashboardFragment.kt:41-51`.
- `androidx.navigation:navigation-compose` was unused (no `NavHost(` in source) and was removed after dependency-analysis proof.

## Screens (7 destinations)

| # | Destination id | Fragment | Compose screen | State source | Domain deps (direct) | Status |
|---|---|---|---|---|---|---|
| 1 | `navigation_profile` (start) | `profile/ProfileSelectionFragment` | `ProfileSelectionScreen.kt` | `MainViewModel` (`activityViewModels`) | `MainViewModel.setProfile` | retain w/ correction |
| 2 | `navigation_dashboard` | `dashboard/DashboardFragment` | `DashboardScreen.kt` | `MainViewModel` (LiveData) | `RecommendationEngine` (via VM), `Calculations` | retain w/ correction |
| 3 | `navigation_calculators` | `calculators/CalculatorsFragment` | `CalculatorsScreen.kt` | `MainViewModel` (LiveData) | `SeachemCalculations`, `SaltMixCalculations` (via VM god-switch `MainViewModel.kt:185-260`) | refactor |
| 4 | `navigation_medication` | `medication/MedicationFragment` | `MedicationScreen.kt` | **in-composable `remember`** (no VM) | `MedicationCatalog`, `MedicationSafetyEngine`, `MedicationSearchEngine` called **inside composable** (`MedicationScreen.kt:118,189-193`) | retain w/ correction |
| 5 | `navigation_fertilizer` | `fertilizer/FertilizerFragment` | `FertilizerScreen.kt` | **in-composable `remember`** (no VM) | `FertilizerChemistryEngine` (in composable) | retain w/ correction |
| 6 | `navigation_settings` | `settings/SettingsFragment` | `SettingsScreen.kt` | `MainViewModel` | `MainViewModel.generateExportData`, `resetAll` | retain unchanged |
| 7 | `navigation_history` | `history/HistoryFragment` | `HistoryScreen.kt` | `HistoryViewModel` (Koin, StateFlow) | `HistoryEventRepository` via ViewModel | wired; needs instrumented/a11y verification |

### Cross-cutting observations
- **Two state patterns coexist**: screens 1–3,6 share the `MainViewModel` god object (LiveData + `SavedStateHandle`); screens 4–5 hold state in `remember{}` and call domain engines straight from the composable. Inconsistent; see `CURRENT_UI_AUDIT.md` ISSUE-ARCH-002 / ISSUE-ARCH-003.
- **Per-feature ViewModel migration is partial** — `HistoryViewModel` exists; legacy
  screens still share `MainViewModel`, while Medication/Fertilizer have no VM.
- **`rememberSaveable` adoption is partial** — History filter state uses it; several
  Medication/Fertilizer screen-local states still use `remember{}` and remain covered
  by ISSUE-STATE-001.
- Profile screen hides bottom nav + app bar (`MainActivity.kt:58-61`); it is a modal-style full-screen entry/switcher, not a tab.

## Screens implied by target product but ABSENT
- No **Catalog/Search** standalone destination (search lives only inside Medication beginner flow `MedicationScreen.kt:188`).
- No **Source/Evidence viewer**, no **Calculation audit viewer** (target architecture asked for these; not built).
