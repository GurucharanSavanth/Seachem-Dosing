# ADR-002: Dependency Injection — Koin

**Status:** Accepted
**Date:** 2026-05-09
**Deciders:** Gurucharan.S
**User decision:** B (Koin)

**Current implementation note (2026-06-29):** Koin is active, but the app is in a
hybrid state. `SeachemDosingApp` starts Koin with `appModule`, `dataModule`, and
`domainModule`; `HistoryViewModel` is Koin-bound; `MainViewModel` remains
AndroidX/SavedStateHandle-instantiated. AI/chat was removed by ADR-010, so there is
no active `aiModule`.

## Context

At ADR creation the app had no dependency injection container. `MainViewModel` was
instantiated by `androidx.lifecycle.ViewModelProvider` directly with
`SavedStateHandle`, and AI clients were inert stubs. The current implementation
keeps `MainViewModel` on the legacy path, binds the History vertical through Koin,
and gates any future AI re-entry through ADR-010.

Forces:
- Koin and Hilt are the two viable Android DI frameworks in 2026; Dagger-only is dated.
- Build times matter — KSP/KAPT chains on Hilt add ~5-15s per clean build.
- Test ergonomics matter — we need mock-easy DI for ViewModel + Repository unit tests.
- Compose interop matters if a composable owns a ViewModel; current ViewModels are still fragment-owned.

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
- ViewModel injection → `viewModel { ... }` in modules, with fragment-owned `by viewModel()` in current code.
- Test fakes → `loadKoinModules(testModule)` swaps bindings.

**Harder:**
- Missing-binding error appears at first use (lazy resolution), not build time. CI must run `verifyAll()` test.
- Feature-flag-gated bindings (per ADR-005 AI hybrid) require `koin-androidx-startup`-style conditional loading.

**Revisit if:**
- Koin runtime startup latency exceeds 50ms on cold launch.
- Multi-module setup grows beyond 5 modules and binding-not-found errors recur.

## Action Items

1. Completed: add Koin 4.0.0 catalog entries and dependencies.
2. Completed: create `SeachemDosingApp : Application()` and register it in
   `AndroidManifest.xml`.
3. Completed: start Koin with `appModule`, `dataModule`, and `domainModule`.
4. Completed for History: bind `HistoryViewModel`, Room database/DAO/repository, and
   the two history write use cases.
5. Partially complete: `KoinVerifyAllTest` statically verifies `appModule`; `dataModule`
   remains excluded from static verify because it requires `androidContext()` and Room.
6. Not current scope: migrate legacy `MainViewModel` to Koin only if/when its
   SavedStateHandle/LiveData path is modernized.
7. Superseded: `aiModule` is not active; ADR-010 controls any future AI/chat re-entry.

## Rollback Criteria

Revert to Hilt if:
- Cold-start latency from Koin startup >50ms (measured via `Log.d("Koin", ...)` timing).
- Three or more runtime missing-binding incidents in CI despite `verifyAll()`.
- Multi-module project structure forces complex `KoinComponent` patterns.
