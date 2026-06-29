# Third-Party Notices

This app bundles third-party open-source software. Versions are pinned in
`gradle/libs.versions.toml` and `app/build.gradle.kts`. License identifiers below use SPDX.

> Licenses are stated to the best of available knowledge; this is not legal advice. For a
> binding bill of materials, generate an SBOM (e.g. CycloneDX Gradle plugin) and have licensing
> reviewed. Items marked **verify** were not asserted to avoid an invented legal conclusion.

## Shipped dependencies (`implementation`) — all Apache-2.0

| Component | License |
|---|---|
| Kotlin standard library (`org.jetbrains.kotlin`) | Apache-2.0 |
| kotlinx-coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-*`) | Apache-2.0 |
| AndroidX Core-KTX, AppCompat, Activity-Compose | Apache-2.0 |
| AndroidX Lifecycle (ViewModel, runtime-compose, viewmodel-compose, livedata) | Apache-2.0 |
| AndroidX Navigation (fragment-ktx, ui-ktx, compose) | Apache-2.0 |
| AndroidX DataStore (preferences) | Apache-2.0 |
| AndroidX ConstraintLayout | Apache-2.0 |
| Jetpack Compose (BOM, ui, ui-graphics, material3, tooling-preview, runtime-livedata) | Apache-2.0 |
| Material Components for Android (`com.google.android.material`) | Apache-2.0 |
| AndroidX Room (runtime, ktx; KSP compiler) | Apache-2.0 |
| Koin (`io.insert-koin:koin-android`, `koin-androidx-compose`) | Apache-2.0 |

## Test-only dependencies (`testImplementation` / `androidTestImplementation`)

| Component | License |
|---|---|
| JUnit 4 (`junit:junit`) | EPL-1.0 |
| MockK (`io.mockk`) | Apache-2.0 |
| Turbine (`app.cash.turbine`) | Apache-2.0 |
| kotlinx-coroutines-test | Apache-2.0 |
| AndroidX Test (ext-junit, espresso, runner, rules), Room-testing, arch-core-testing | Apache-2.0 |
| Compose UI Test (`androidx.compose.ui:ui-test-junit4`, `ui-test-manifest`) | Apache-2.0 |
| `org.json:json` | **verify** (org.json's "JSON License" / recent releases; not asserted here) |

## Build tooling

| Component | License |
|---|---|
| Android Gradle Plugin, Gradle | Apache-2.0 |
| Kotlin Symbol Processing (KSP) | Apache-2.0 |

## Domain data / formula attribution

Dosing and chemistry coefficients are derived from manufacturer labels and chemistry references;
in-app source/evidence attribution and the formula provenance basis are documented in
`DEEP_RESEARCH_REPORT.md` and ADRs. No third-party datasets are bundled.
