# UI ↔ Domain Connection Matrix — v2.0-wip

Which built domain/data artifacts are actually reachable from the UI. Originally
grep-confirmed at commit `1d0744f`; current implementation notes were refreshed on
2026-06-29 after History wiring and AI removal.

Legend: **Wired** = consumed by a screen/VM on a live path · **Orphan** = built + (maybe) DI-registered but no UI consumer · **In-composable** = called directly from a `@Composable` (architecture smell).

| Domain / data artifact | File | Reaches UI? | Consumer | Finding |
|---|---|---|---|---|
| `SeachemCalculations` | `logic/` | ✅ Wired | `MainViewModel.calculateUniversal` (`MainViewModel.kt:185`) | OK, but via 26-arm `when` in the VM, not a use case. |
| `SaltMixCalculations` | `logic/` | ✅ Wired | `MainViewModel.calculateSaltMix` | OK. |
| `Calculations` | `logic/` | ✅ Wired | `MainViewModel` (prime/stability/safe/APT/water-change) | OK. |
| `RecommendationEngine` | `domain/engine/` | ✅ Wired | Dashboard (via VM) | OK. |
| `MedicationCatalog` / `MedicationSafetyEngine` / `MedicationSearchEngine` | `domain/medication/` | ⚠️ In-composable | `MedicationScreen.kt:93,106,118,189-193` | ISSUE-CONN-001 (med): domain called straight from the composable, no VM/use-case boundary. |
| `FertilizerChemistryEngine` | `domain/engine/` | ⚠️ In-composable | `FertilizerScreen` | ISSUE-CONN-001. |
| `ValidationEngine` | `domain/engine/` | ❓ Verify | DI only? | Check for live consumer; VM uses inline `coerceX` instead (`MainViewModel.kt:453-461`). |
| `UnitConversionEngine` | `domain/engine/` | ❓ Verify | DI only? | VM uses `Calculations.ppmToDh/dhToPpm` directly (`MainViewModel.kt:325,352`). Possible duplication/orphan. |
| `LogAdministeredDoseUseCase` | `domain/usecase/` | ✅ Wired | `CalculatorsFragment` via Koin `inject()` | Logs explicit "Log as administered" actions to History. |
| `RecordWaterParameterReadingUseCase` | `domain/usecase/` | ✅ Wired | `DashboardFragment` → `WaterReadingsRecorder` | Saves dashboard water readings to History. |
| `HistoryEventRepository` / `HistoryEventRepositoryImpl` | `data/repository/` | ✅ Wired | `HistoryViewModel`, history write use cases | ISSUE-CONN-003 resolved by ADR-008/011 implementation. |
| `HistoryDao` / `AppDatabase` (Room v2) | `data/local/` | ✅ Wired | `HistoryEventRepositoryImpl` | v2 append-only event schema backs History read/write paths. |
| AI/chat package/resources | `ai/`, chat resources | ✅ Removed | ADR-010 | ISSUE-CONN-004 resolved; future AI re-entry is documentation-gated. |

## Orphan resources
The former AI/chat resources have been removed. Continue using lint/grep before any
future resource deletion.

## Dead VM code (verify then remove)
`MainViewModel.kt:145-156` legacy calculator state holders + `:605+` legacy setters
remain. Confirm no references, then delete in a focused cleanup (ISSUE-ARCH-004).

## Net
History is no longer orphaned, AI/chat dead code is removed, and the unused
settings repository staging layer was deleted. Remaining wiring debt:
Medication/Fertilizer still call domain engines inside composables, and legacy
`MainViewModel` holders/setters need deletion only after grep/lint proof.
