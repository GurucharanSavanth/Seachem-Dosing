# Cleanup Execution Ledger

Baseline: branch `v2.0-wip`, start `16650b7cb36a045b733c4c3fd98e4ede2537e3c5`.

## Preflight

| Check | Result |
|---|---|
| `git status --short` | clean |
| `git diff --check` | pass |
| tracked files | 246 |
| tracked lines | 55,543 |
| repo size | 547,348 KiB |
| `./gradlew testDebugUnitTest` | pass |
| `./gradlew lintDebug` | pass |
| `./gradlew assembleDebug` | pass |
| `./gradlew assembleRelease` | pass |
| `node scripts/verify-sync.js` | pass |
| `web`: `npm ci`, typecheck, lint, test, build | pass |

Verbose logs: `build/reports/cleanup/`.

## Candidate Decisions

| Candidate | Current purpose | References found | Decision | Reason | Replacement | Tests required | Commit |
|---|---|---|---|---|---|---|---|
| `existing workflow by claude befor compacting.txt` | Ignored conversation transcript | none | delete | Not tracked; no build/runtime/docs role. | none | `git status`, docs grep | f2b3974 |
| `AGENT.md` | Generic AI-agent prompt | self-reference only | delete | Not project architecture or user-facing documentation. | none | `git status`, docs grep | f2b3974 |
| `AGENT_WORK_LOG.md` | Agent fan-out notes | none outside transient docs | delete | Duplicates conclusions now captured in research/audit docs. | `DEEP_RESEARCH_REPORT.md` | `git status`, docs grep | f2b3974 |
| `CURRENT_STATE.md` | Historical checkpoint summary | audit docs and `PHASE9_VERIFICATION.md` | delete | Stale state doc; current evidence belongs in this ledger. | `docs/audit/CLEANUP_EXECUTION_LEDGER.md` | docs grep | f2b3974 |
| `DEEP_RESEARCH_REPORT.md` | Formula, Android, medication evidence | SPEC, ADRs, product schema, code/tests, notices | retain | Durable formula/provenance evidence. | n/a | n/a | n/a |
| `FINAL_QA_REPORT.md` | Final QA/security summary | audit docs | retain | Final QA/security report is explicitly retained evidence. | n/a | n/a | n/a |
| `PHASE9_VERIFICATION.md` | Release/security/parity verification | direct release evidence | retain | Release verification report is explicitly retained evidence. | n/a | n/a | n/a |
| `MODERNIZATION_PLAN.md` | Accepted migration plan | SPEC, phase matrix, final QA | retain | Still referenced as planning/source-of-truth history. | n/a | n/a | n/a |
| `TOOL_FAILURE_LOG.md` | Curated environment/tool failure ledger | deep research, tool ledger, audit docs | retain | Not raw output; documents unresolved/retestable tool constraints. | n/a | n/a | n/a |
| `app/release/` | Tracked generated v1.0 APK, baseline profile `.dm`, output metadata | stale metadata only; docs reference `app/build/outputs/...` for current release output | delete | Generated release outputs belong in build/release artifacts, not source. No tracked key material; history only contains old default Android debug password literals. | `./gradlew assembleRelease` output + CI artifacts | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` | 6b88ab8 |
| `docs/sbom/bom.cdx.json` | Hand-authored machine-readable SBOM | docs only | delete | Reproducible generated SBOM now comes from `scripts/generate-sbom.js` and CI upload. | `build/reports/sbom/bom.cdx.json` artifact | `node scripts/generate-sbom.js`, Gradle gates | 7ea6bf3 |
| `web/`, `Base_Template/` | Dual web stacks | README, CI, ADR-004, tests, parity script | skipped | User instructed to skip and ignore both web directories for cleanup. | n/a | n/a | n/a |
| `Feature_Update/` | Orphaned salt-mix JS drop | ADR/web comments only; no build, CI, script, import, release, or submodule use | delete | Salt-mix factors are already in active Kotlin and TS engine code. | `SaltMixCalculations.kt`; `web/src/engine/salt-mix.ts` | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`, `web typecheck` | 92984e7 |
| `app/src/main/java/com/example/seachem_dosing/domain/model/` | Future/staged domain DTOs | Definitions only; stale `SPEC.md`, `MODERNIZATION_PLAN.md`, `PHASE_MATRIX.md`, and one web comment | delete | No source, test, DI, Room, serialization, reflection, manifest, XML, R8, CI, or release role. | `core/result/CalcResult`; current `domain/engine`, `domain/history`, `domain/usecase` classes | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` | pending |
| `SettingsRepository.kt` + DataStore dependency | Planned persistent settings abstraction | Interface and Gradle dependency only; no implementation, DI binding, tests, callers, migration, manifest, XML, reflection, or release use | delete | Current app intentionally uses SharedPreferences for selected profile plus SavedStateHandle/in-memory UI state. | Existing `ProfileSelectionFragment` SharedPreferences and `MainViewModel` SavedStateHandle paths | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` | pending |
| Android deps: `navigation-compose`, `koin-androidx-compose`, `lifecycle-viewmodel-compose`, `mockk`, `turbine` | Staged/future Compose nav, DI helper, ViewModel helper, and test helpers | Gradle/docs only; no imports or source/test call sites | delete | Fragment Navigation, AndroidX fragment `viewModel`, JUnit, Koin tests, and coroutines-test cover current code. | Existing Fragment Navigation, `koin-android`, `koin-test`, `kotlinx-coroutines-test` | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease`, `connectedDebugAndroidTest` if adb exists | pending |
| Android dep: `espresso-core` | Android test runtime dependency | Direct app dependency plus transitive Compose UI test dependency; direct pin upgrades Compose's transitive 3.5.0 to 3.7.0 | retain | Required to keep API-36 Compose UI test retest path on current AndroidX Test runtime; no source import expected. | n/a | `dependencyInsight`; instrumented test when adb exists | n/a |
