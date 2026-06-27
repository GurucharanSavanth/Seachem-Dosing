# ADR-002: Dependency Injection — Koin

**Status:** Accepted
**Date:** 2026-05-09
**Deciders:** Gurucharan.S
**User decision:** B (Koin)

## Context

The app currently has no dependency injection container. `MainViewModel` is instantiated by `androidx.lifecycle.ViewModelProvider` directly with `SavedStateHandle`. AI clients (`GeminiClient`) are stubs but already take constructor params (`apiKey`, `model`). Phase 4 adds Repository + UseCase layers per ADR-003, plus Room DAOs and AI implementations — each needs lifecycle-scoped construction.

Forces:
- Koin and Hilt are the two viable Android DI frameworks in 2026; Dagger-only is dated.
- Build times matter — KSP/KAPT chains on Hilt add ~5-15s per clean build.
- Test ergonomics matter — we need mock-easy DI for ViewModel + Repository unit tests.
- Compose interop matters — both have first-class `koinViewModel()` / `hiltViewModel()` helpers.

## Decision

Adopt **Koin** as DI framework, using DSL-based modules.

## Options Considered

### Option A — Hilt
Google's recommended DI; annotation-based; KSP-compiled bindings.

| Dimension | Score (1-5) |
|---|---|
| Build time | 2 (KSP overhead) |
| Test ergonomics | 4 (HiltAndroidRule mature) |
| Ecosystem | 5 |
| Learning curve | 3 (annotations + scopes) |
| Compile-time safety | 5 (errors at build) |

**Pros:** Compile-time DI graph validation. AOSP backing. Massive ecosystem.
**Cons:** Slower builds. `@HiltViewModel` + `@Inject` boilerplate. Module bindings can get verbose with `@InstallIn(SingletonComponent::class)`.

### Option B — Koin (CHOSEN)
Pure Kotlin DSL; runtime resolution; no codegen.

| Dimension | Score (1-5) |
|---|---|
| Build time | 5 (no codegen) |
| Test ergonomics | 5 (KoinTest, declareMock) |
| Ecosystem | 4 |
| Learning curve | 4 (idiomatic Kotlin DSL) |
| Compile-time safety | 3 (runtime errors; mitigated by koin-test verifyAll) |

**Pros:** No KSP/KAPT chain. Faster builds. DSL reads as code, not annotations. `viewModel { }`, `single { }`, `factory { }` are clear. `androidContext()` + `androidLogger()` standard.
**Cons:** Errors at app startup (can be missing-binding panic). Mitigated by `verifyAll()` in CI test.

### Option C — Manual DI with factories
Continue without DI framework; use factory pattern for ViewModel and Repository creation.

| Dimension | Score (1-5) |
|---|---|
| Build time | 5 |
| Test ergonomics | 2 (manual mock wiring) |
| Ecosystem | 1 |
| Learning curve | 5 |
| Compile-time safety | 5 |

**Pros:** Zero new dep. Explicit.
**Cons:** Boilerplate scales linearly with class count. We're adding 10+ classes Phase 4. Won't survive feature growth.

## Trade-off Analysis

Hilt's compile-time safety advantage is real but pays per-build cost forever. Koin's runtime-error risk is mitigated by `koin-test`'s `verifyAll()` — runs in CI, catches missing bindings as a unit test failure rather than a runtime panic. Manual DI doesn't scale to v2.0 layer count.

User chose B (Koin). Build-time win and DSL readability outweigh compile-time safety loss given test mitigation.

## Consequences

**Easier:**
- Adding a new Repository/UseCase → one `single { }` line in module.
- ViewModel injection → `viewModel { MainViewModel(get(), get()) }` + `koinViewModel()` in Composable.
- Test fakes → `loadKoinModules(testModule)` swaps bindings.

**Harder:**
- Missing-binding error appears at first use (lazy resolution), not build time. CI must run `verifyAll()` test.
- Feature-flag-gated bindings (per ADR-005 AI hybrid) require `koin-androidx-startup`-style conditional loading.

**Revisit if:**
- Koin runtime startup latency exceeds 50ms on cold launch.
- Multi-module setup grows beyond 5 modules and binding-not-found errors recur.

## Action Items

1. Add to `gradle/libs.versions.toml`:
   - `io.insert-koin:koin-android:3.5.6`
   - `io.insert-koin:koin-androidx-compose:3.5.6` (when Compose mig starts)
   - `io.insert-koin:koin-test:3.5.6` (testImplementation)
2. Create `SeachemDosingApp : Application()` registered in `AndroidManifest.xml` via `android:name`.
3. In `Application.onCreate()`: `startKoin { androidContext(this@SeachemDosingApp); modules(appModule, dataModule, domainModule) }`.
4. Define modules:
   - `appModule` — `SettingsManager`, theme/locale state.
   - `dataModule` — Room database, DAOs, repositories.
   - `domainModule` — UseCases.
   - `aiModule` — Gemini/Local LLM clients (per ADR-005), feature-flag-gated.
5. Migrate `MainViewModel` to `viewModel { MainViewModel(get(), get(), get()) }` and use `koinViewModel()` in fragments → composables.
6. Add `KoinVerifyAllTest` to `app/src/test/`: `koinApplication { modules(allModules) }.checkModules()`.

## Rollback Criteria

Revert to Hilt if:
- Cold-start latency from Koin startup >50ms (measured via `Log.d("Koin", ...)` timing).
- Three or more runtime missing-binding incidents in CI despite `verifyAll()`.
- Multi-module project structure forces complex `KoinComponent` patterns.
