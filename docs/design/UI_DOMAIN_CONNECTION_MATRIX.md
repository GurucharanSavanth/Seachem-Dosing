# UI ↔ Domain Connection Matrix — v2.0-wip

Which built domain/data artifacts are actually reachable from the UI. Grep-confirmed at commit `1d0744f`. No code changed.

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
| `CalculateDoseUseCase` | `domain/usecase/` | ❌ **Orphan** | none (grep: only `usecase/` + `di/`) | ISSUE-CONN-002 (high): use-case layer built + DI-wired but **no UI calls it**. |
| `CalculateQuickDoseUseCase` | `domain/usecase/` | ❌ Orphan | none | ISSUE-CONN-002. |
| `ConvertUnitsUseCase` | `domain/usecase/` | ❌ Orphan | none | ISSUE-CONN-002. |
| `ValidateInputUseCase` | `domain/usecase/` | ❌ Orphan | none | ISSUE-CONN-002. |
| `RecordDoseUseCase` | `domain/usecase/` | ❌ Orphan | none | ISSUE-CONN-002. |
| `HistoryRepository` / `HistoryRepositoryImpl` | `data/repository/` | ❌ **Orphan** | no `ui/` reference (grep) | ISSUE-CONN-003 (high): history persistence exists, **no History screen** surfaces it. |
| `DosingLogDao` / `ParameterLogDao` / `AppDatabase` (Room) | `data/local/` | ❌ Orphan | only via orphan repo/usecase | ISSUE-CONN-003. Room schema + KSP cost with no user-visible feature. |
| `CalculationsRepository(Impl)` | `data/repository/` | ❓ Verify | DI | Confirm a live consumer; may be orphan behind the unused use cases. |
| `SettingsRepository` | `data/repository/` | ❓ Verify | Settings? | Verify Settings screen uses it vs. `MainViewModel`. |
| `GeminiClient` / `AiModels` / `ChatMessage` / `AiInsightState` | `ai/` | ❌ **Inert/Orphan** | only dead `MainViewModel` state (`MainViewModel.kt:41-45`); no screen renders chat | ISSUE-CONN-004 (med): AI stubbed (`isConfigured()=false` per CLAUDE.md); VM still holds `aiInsight`/`chatMessages`; resources `item_chat_message.xml`, `bg_chat_*`, `menu/context_recommendations.xml` unreferenced. |

## Orphan resources (no `.kt`/layout reference — verify then remove)
`res/layout/item_chat_message.xml`, `res/drawable/bg_chat_user.xml`, `res/drawable/bg_chat_assistant.xml`, `res/menu/context_recommendations.xml`. Candidates for ISSUE-CONN-004 cleanup **after** a final `lint`/`UnusedResources` pass.

## Dead VM code (verify then remove)
`MainViewModel.kt:152-164` "Legacy calculator state holders" + `:615-627` "Legacy setters (kept for compatibility if referenced)" — comment admits conditional liveness; fragments referencing them are gone (now Compose). Confirm no references, then delete (ISSUE-ARCH-004).

## Net
Two whole layers (`domain/usecase/*`, `data/**` history + AI) are **built but unreachable from the UI**. Either connect them (add History screen; route dosing through use cases) or remove them. This is the single biggest "modernized on paper, not wired" gap. Decisions belong in `TARGETED_UI_REMEDIATION_PLAN.md`; **do not delete before confirming non-reference and owner sign-off.**
