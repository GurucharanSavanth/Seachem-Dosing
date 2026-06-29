# Documentation Truth Audit

**Date:** 2026-06-29
**Base commit audited:** `756d944` (`docs: align tech stack with current architecture`)
**Latest correction baseline:** `996c71f` (`docs: correct stale architecture claims`)
**Scope:** root README, module READMEs, ADRs, testing/security/release docs, web docs, CI, Gradle config, and current implementation evidence.

## Summary

The root README Tech Stack rows were corrected before this audit. High-severity drift
in ADR-003, ADR-008, ADR-011, QA, testing, security, and web-status docs was corrected
through `996c71f`. Remaining known drift is concentrated in ADR-002 hybrid DI wording,
dependency catalog comments/Material alias state, and the tool-failure follow-up note.

Do not treat old planning docs as current state unless this audit marks the claim verified.

## Correction Status

| Finding IDs | Status | Correction evidence |
|---|---|---|
| DOC-001, DOC-002, DOC-009, DOC-010 | corrected | `docs/architecture/adr-003-database.md`, `docs/architecture/adr-008-history-feature.md`, and `docs/architecture/adr-011-precision-safe-history-schema.md` now label v1/destructive/schema-converter text as superseded or current implementation-specific. |
| DOC-003, DOC-004, DOC-005, DOC-006, DOC-012, DOC-013, DOC-019, DOC-022, DOC-023, DOC-024 | corrected | `CURRENT_STATE.md`, `FINAL_QA_REPORT.md`, `TESTING_STRATEGY.md`, and `SECURITY.md` now use current Gradle/test evidence and avoid unsupported CI, signing, secret-scan, storage, and test-count claims. |
| DOC-007, DOC-008, DOC-014, DOC-015, DOC-016, DOC-017, DOC-025, DOC-026 | corrected | `web/README.md`, `web/package.json`, `docs/architecture/adr-004-web-stack.md`, `Base_Template/README.md`, and `README.md` now describe web v2 as an alpha scaffold, PWA as a target, sync-check as legacy-only, and `Base_Template/` as the legacy static app. |
| DOC-011, DOC-018, DOC-020, DOC-021 | open | Remaining remediation: ADR-002 hybrid DI/AI action drift, `TOOL_FAILURE_LOG.md` Espresso follow-up, stale dependency-catalog comments, and Material alias/config drift. |

## Critical Corrections Already Applied

| Claim | Prior state | Current evidence | Status |
|---|---|---|---|
| Android architecture row | `MVVM + LiveData` | `MainViewModel` still uses LiveData/SavedStateHandle; `HistoryViewModel` uses StateFlow/Flow | corrected in `README.md` |
| UI row | `Fragment Navigation + ViewBinding` | Fragment/XML nav shell remains; screens hosted by `ComposeView`; ViewBinding remains in `MainActivity` | corrected in `README.md` |
| Storage row | `DataStore Preferences` | Room v2 is wired for history; DataStore is staged only; active settings/profile use SavedStateHandle + SharedPreferences | corrected in `README.md` |

## Findings

| ID | Severity | Classification | Claim / location | Evidence | Recommended action | Test implications |
|---|---|---|---|---|---|---|
| DOC-001 | High | obsolete | `docs/architecture/adr-003-database.md` schema sketch uses v1 `parameter_log` / `dosing_log` and says no migrations needed. | `app/src/main/java/com/example/seachem_dosing/data/local/database/AppDatabase.kt` has `@Database(version = 2)` with `history_event`, `dose_event_detail`, `parameter_event_detail`; ADR-011 defines non-destructive `Migration(1,2)`. | Mark ADR-003 v1 schema section superseded by ADR-011. | Migration tests should target v2 event schema, not v1 "no migration" assumptions. |
| DOC-002 | High | contradictory | `docs/architecture/adr-008-history-feature.md` includes destructive delete/clear-all behavior. | `HistoryDao.kt` and `HistoryEventEntity.kt` implement append-only correction/void semantics; ADR-011 rejects silent mutation. | Amend ADR-008 as superseded for destructive deletion by ADR-011. | UI/DAO tests should assert no physical delete path and verify void/correction display. |
| DOC-003 | High | stale / conflicting | `CURRENT_STATE.md`, `FINAL_QA_REPORT.md`, `TESTING_STRATEGY.md` hard-code incompatible JVM/instrumented test counts. | Current test tree has 24 JVM test files and 5 androidTest files; recent `./gradlew testDebugUnitTest` passed at `756d944`, but report counts differ across docs. | Replace hard-coded counts with last-run command/result plus generated report location, or regenerate counts from Gradle reports. | Avoid release decisions based on stale counts such as 36/54/142. |
| DOC-004 | High | unsupported | `TESTING_STRATEGY.md` says Compose UI test runs on API <=34 / CI. | `.github/workflows/ci.yml` runs unit tests, lint, and debug APK only; no emulator or `connectedDebugAndroidTest` job. | Remove `/ CI` claim or add an emulator instrumented-test job. | Compose UI assertions are not CI-gated today. |
| DOC-005 | High | overstated | `CURRENT_STATE.md` presents release gate / signed release APK as current general state. | `app/build.gradle.kts` signs release only when all `SEACHEM_RELEASE_*` values are present; CI does not run `assembleRelease`; `versionName = "2.0"` while doc names v1.0 artifact. | State release verification as local-only historical evidence unless rerun; qualify signing as conditional. | Release validation should derive APK name from Gradle, not stale docs. |
| DOC-006 | High | unsupported | `FINAL_QA_REPORT.md` claims a secret-staging guard runs on every commit. | `TOOL_UTILIZATION_LEDGER.md` says repo guardrails are not configured; no CI secret scan is present. | Either configure a secret scan/pre-commit guard or downgrade claim to "ignored/untracked and not committed". | No automated build failure currently prevents secret staging. |
| DOC-007 | High | unsupported / planned | `web/package.json` and `web/README.md` call `web/` a PWA. | `web/README.md` still marks manifest/service worker incomplete; `web/vite.config.ts` references icons under absent `web/public/`. | Say "PWA scaffold" until manifest/assets/service worker and install/offline checks exist. | Add PWA build/offline smoke before claiming installability. |
| DOC-008 | High | false parity claim | `web/README.md` says CI hard-gates calculation parity via `scripts/verify-sync.js`. | `scripts/verify-sync.js` checks `Base_Template/js/utils.js` and `Base_Template/js/dosingCalculations.js`; it explicitly excludes `SeachemCalculations.kt` parity and does not inspect `web/src/**`. | Document sync check as legacy-only, or expand it to cover `web/src/**`. | Add `web/tests/sync-validation.test.ts` or equivalent golden parity vectors. |
| DOC-009 | Medium | partially current | ADR-011 schema block names `correction_of_event_id` / `voided`. | Implemented schema uses `supersedes_event_id`, `voids_event_id`, and `correction_reason` in `HistoryEventEntity.kt`. | Update ADR-011 schema block to implementation names. | Schema assertions should use implemented columns and event codes. |
| DOC-010 | Medium | unsupported | ADR-011 describes a Room converter read path for `StoredDecimal`. | `StoredDecimalConverter.kt` exists, but entities persist decimal fields as `String`; `AppDatabase.kt` has no `@TypeConverters`. | Document "validated string storage" or wire converter and typed fields. | DAO tests cannot assume malformed decimal strings are rejected by Room converter. |
| DOC-011 | Medium | partially current | ADR-002 says no DI container and lists future AI module actions. | `SeachemDosingApp.kt` starts Koin; `DataModule.kt` and `DomainModule.kt` are active; `MainViewModel` remains AndroidX; ADR-010 gates AI re-entry. | Amend ADR-002 with current hybrid DI state and remove/gate AI module action items. | Koin verify should cover current modules, not require `MainViewModel` or AI bindings. |
| DOC-012 | Medium | inaccurate data handling | `SECURITY.md` says data is stored in Room / DataStore. | Room is wired for history; DataStore has dependency/interface only; `ProfileSelectionFragment.kt` uses SharedPreferences; `MainViewModel` uses SavedStateHandle. | State Room + SharedPreferences/SavedStateHandle; mention DataStore as staged only. | Privacy/backup review must include SharedPreferences profile state. |
| DOC-013 | Medium | stale release artifact | `CURRENT_STATE.md` names `SeachemDosing-v1.0-release.apk`. | `app/build.gradle.kts` has `versionName = "2.0"` and variant output naming based on current version. | Remove stale artifact path or update after rerunning release build. | Prevent testing/release of wrong APK. |
| DOC-014 | Medium | stale web status | `web/README.md` says 14 Android-only calculators are unported. | `web/src/engine/types.ts` and `web/tests/engine.test.ts` include Flourish/Reef product dispatch, but `web/src/main.ts` still routes users to legacy `Base_Template/`. | Document: engine ports partial; UI/export/sync parity pending. | Replace smoke-only finite tests with Kotlin golden parity tests. |
| DOC-015 | Medium | stale CI claim | ADR-004 says CI should run `npm ci`, lint, test, and build for web. | `.github/workflows/ci.yml` runs `npm install`, `npm run typecheck`, and `npm run lint`; no web test/build gate. | Add lockfile + CI test/build, or amend ADR as target state. | `web/` Vitest/build regressions are not CI-gated. |
| DOC-016 | Medium | stale tests/lint docs | `Base_Template/README.md` documents `npm ci`, Jest, tests, Airbnb/security ESLint. | No `Base_Template/package.json` or `Base_Template/tests/`; web tests live under `web/` and use Vitest. | Remove npm/Jest/Airbnb/security claims or label the file as legacy historical docs. | No Base_Template test command exists unless added. |
| DOC-017 | Medium | target-vs-current ambiguity | ADR-004 says `web/` replaces `Base_Template/`. | Root README still points quick-start web users to `Base_Template`; `web/src/main.ts` tells users to use legacy until Phase 5 completes. | Mark ADR-004 replacement as target state; keep current docs explicit that Base_Template is the usable legacy web app. | Smoke target remains Base_Template until web UI parity exists. |
| DOC-018 | Medium | stale remediation | `TOOL_FAILURE_LOG.md` says test Espresso 3.7 alpha when stable. | Current catalog has Espresso 3.6.1; AndroidX Test 3.7.0 is now stable per official AndroidX Test release page. | Decide whether to upgrade/test Espresso 3.7.0, or keep AVD workaround and update note. | API-36 Compose UI blockage may be dependency-verifiable now. |
| DOC-019 | Medium | unsupported command | `TESTING_STRATEGY.md` uses placeholder `-P...class=ProfileSelectionScreenTest`. | Correct runner arg is `-Pandroid.testInstrumentationRunnerArguments.class=com.example.seachem_dosing.ui.profile.ProfileSelectionScreenTest`. | Replace placeholder with runnable command. | Current doc command is not a reliable single-class instrumented test filter. |
| DOC-020 | Low | obsolete comments | `gradle/libs.versions.toml` comments say Compose/Koin/Room entries are declared but not referenced / activate later. | `app/build.gradle.kts` applies Compose plugin, KSP, Koin, and Room dependencies. | Update comments to "active" or remove stale comments. | Dependency audits should treat them as active dependencies. |
| DOC-021 | Low | build config drift | `gradle/libs.versions.toml` has Material `1.10.0`; app uses hardcoded Material `1.11.0`. | `app/build.gradle.kts` line for `com.google.android.material:material:1.11.0`. | Use `libs.material` with current version or delete stale alias. | Version reports can misstate the active Material XML dependency. |
| DOC-022 | Low | stale test inventory | `TESTING_STRATEGY.md` references `CalculateDoseUseCaseTest`. | No such file exists; current use-case tests include `HistoryWriteUseCasesTest`. | Replace with current test suite names. | Coverage map points reviewers to missing tests. |
| DOC-023 | Low | ambiguous smoke scope | `FINAL_QA_REPORT.md` says "all 5 screens"; `TESTING_STRATEGY.md` table lists six screens and newer History screen exists. | Navigation graph includes Profile, Dashboard, Calculators, Medication, Fertilizer, Settings, History. | List exact screens verified, including History status. | Manual smoke coverage should not rely on stale counts. |
| DOC-024 | Low | stale compliance blocker | `CURRENT_STATE.md` says no LICENSE is present. | `LICENSE` and `NOTICE` exist and README now says Apache-2.0. | Remove license blocker; keep owner/legal review if needed. | No test impact. |
| DOC-025 | Low | ambiguous deployment docs | `Base_Template/README.md` links a live demo without clarifying it is the legacy static app. | `Base_Template/index.html` identifies the static app; `web/README.md` is v2 alpha bootstrap. | Label demo as legacy until Vite web deploy exists. | Add deploy/link smoke only if web release is in scope. |
| DOC-026 | Low | stale issue links | Root README still links `Seachem-Calculatore` issues in contributing/footer. | Git remote is `https://github.com/GurucharanSavanth/Seachem-Dosing.git`. | Update issue links to `Seachem-Dosing`. | No test impact. |

## Verified Current Claims

| Claim | Evidence |
|---|---|
| Android floor is API 33 / Android 13.0. | `app/build.gradle.kts` `minSdk = 33`; `README.md` badge/table; ADR-006. |
| Kotlin plugin version is 2.0.21. | `gradle/libs.versions.toml` `kotlin = "2.0.21"`. |
| Java toolchain target is 17. | `app/build.gradle.kts` `sourceCompatibility`, `targetCompatibility`, `kotlinOptions.jvmTarget`. |
| Fragment/XML navigation shell remains. | `activity_main.xml` `FragmentContainerView`; `mobile_navigation.xml`; `MainActivity.kt` `NavHostFragment`. |
| Compose screens are hosted through `ComposeView`. | `DashboardFragment`, `CalculatorsFragment`, `ProfileSelectionFragment`, `SettingsFragment`, `MedicationFragment`, `FertilizerFragment`, `HistoryFragment`. |
| AI/chat implementation is removed/gated. | `adr-010-remove-ai-chat.md`; no `app/src/main/java/**/ai` package in current file list. |
| Apache-2.0 license file exists. | `LICENSE`, `NOTICE`, root README license badge. |

## Next Correction Queue

1. Fix high-severity documentation lies first: ADR-003, ADR-008, test/release/security overclaims, web PWA/parity claims.
2. Then update medium-severity state docs: CURRENT_STATE, SECURITY storage wording, ADR-002/011, TESTING_STRATEGY commands.
3. Then clean low-risk drift: version-catalog comments, Material alias, README stale issue links, Base_Template legacy labels.

## Tool / Agent Use

| Tool | Status | Technical reason |
|---|---|---|
| Ponytail | used | active mode; constrained audit to factual corrections and shortest useful report. |
| test-android-apps:android-performance | loaded, not used | no focused performance flow requested yet. |
| test-android-apps:android-emulator-qa | loaded, not used | documentation audit did not require emulator driving. |
| multi-agent explorers | used | three bounded read-only slices: architecture, Android/build, web. |
| Figma / Claude Design | not applicable | UI design review was not needed for documentation truth classification. |
| Supabase / freecad / UI5 | not applicable | repository has no Supabase, CAD, or UI5 dependency/scope. |

## Handoff

**Current correction baseline:** `996c71f`
**Completed work:** Tech Stack README correction, repository-wide documentation truth audit, phase matrix, and high-severity documentation corrections.
**Tests passed since audit start:** `./gradlew testDebugUnitTest`, `./gradlew lintDebug`, `./gradlew assembleDebug`; `testDebugUnitTest` was rerun after the high-severity docs batch.
**Active risks:** DOC-011, DOC-018, DOC-020, and DOC-021 remain open; unrelated local edits remain in `.gitignore` and `.idea/appInsightsSettings.xml`.
**Next executable task:** apply the remaining medium/low documentation corrections, then update the phase matrix from current code/docs.
