# Software Bill of Materials (SBOM) — Seachem-Dosing v2.0

**Artifact:** `app/build/outputs/apk/release/SeachemDosing-v2.0-release.apk`
**Generated:** 2026-06-29 · **Scope:** declared (direct) dependencies + build toolchain.
**Source of truth:** `gradle/libs.versions.toml`, `app/build.gradle.kts`.

> This is a **direct-dependency** SBOM at the declared level. It is sufficient for an
> offline, no-backend hobby app with no distribution-supply-chain obligations.
> For a machine-readable CycloneDX/SPDX document with the full transitive graph,
> add the `cyclonedx-gradle-plugin` (`org.cyclonedx.bom`) and run `:app:cyclonedxBom`.
> The web app (`Base_Template/`) ships **zero runtime dependencies**; the `web/`
> TypeScript scaffold has build-time devDependencies only (not shipped) — see end.

## Build toolchain

| Component | Version | License |
|---|---|---|
| Android Gradle Plugin (`com.android.application`) | 8.13.2 | Apache-2.0 |
| Kotlin (`org.jetbrains.kotlin.android`, `kotlin.plugin.compose`) | 2.0.21 | Apache-2.0 |
| KSP (`com.google.devtools.ksp`) | 2.0.21-1.0.28 | Apache-2.0 |
| Java toolchain (compile/target) | 11 | — |
| Android SDK | compileSdk 36.1 · minSdk 33 · targetSdk 36 | — |

## Runtime dependencies (shipped in APK — `implementation`)

| Coordinate | Version | License |
|---|---|---|
| androidx.core:core-ktx | 1.10.1 | Apache-2.0 |
| androidx.appcompat:appcompat | 1.6.1 | Apache-2.0 |
| com.google.android.material:material | 1.11.0 | Apache-2.0 |
| androidx.constraintlayout:constraintlayout | 2.1.4 | Apache-2.0 |
| androidx.navigation:navigation-fragment-ktx | 2.7.7 | Apache-2.0 |
| androidx.navigation:navigation-ui-ktx | 2.7.7 | Apache-2.0 |
| androidx.lifecycle:lifecycle-livedata-ktx | 2.7.0 | Apache-2.0 |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.7.0 | Apache-2.0 |
| androidx.datastore:datastore-preferences | 1.0.0 | Apache-2.0 |
| androidx.compose:compose-bom | 2024.12.01 | Apache-2.0 |
| androidx.compose.ui:ui *(BOM-managed)* | — | Apache-2.0 |
| androidx.compose.ui:ui-graphics *(BOM-managed)* | — | Apache-2.0 |
| androidx.compose.ui:ui-tooling-preview *(BOM-managed)* | — | Apache-2.0 |
| androidx.compose.material3:material3 *(BOM-managed)* | — | Apache-2.0 |
| androidx.compose.runtime:runtime-livedata *(BOM-managed)* | — | Apache-2.0 |
| androidx.activity:activity-compose | 1.9.3 | Apache-2.0 |
| androidx.lifecycle:lifecycle-runtime-compose | 2.8.7 | Apache-2.0 |
| androidx.lifecycle:lifecycle-viewmodel-compose | 2.8.7 | Apache-2.0 |
| androidx.navigation:navigation-compose | 2.8.5 | Apache-2.0 |
| io.insert-koin:koin-android | 4.0.0 | Apache-2.0 |
| io.insert-koin:koin-androidx-compose | 4.0.0 | Apache-2.0 |
| androidx.room:room-runtime | 2.6.1 | Apache-2.0 |
| androidx.room:room-ktx | 2.6.1 | Apache-2.0 |

## Build-time only (annotation processor — not shipped)

| Coordinate | Version | License | Scope |
|---|---|---|---|
| androidx.room:room-compiler | 2.6.1 | Apache-2.0 | KSP |

## Test dependencies (not shipped — `testImplementation` / `androidTestImplementation`)

| Coordinate | Version | License |
|---|---|---|
| junit:junit | 4.13.2 | EPL-1.0 |
| androidx.arch.core:core-testing | 2.2.0 | Apache-2.0 |
| org.json:json | 20240303 | JSON License (test-only; not shipped) |
| io.mockk:mockk | 1.13.13 | Apache-2.0 |
| app.cash.turbine:turbine | 1.1.0 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-test | 1.9.0 | Apache-2.0 |
| io.insert-koin:koin-test | 4.0.0 | Apache-2.0 |
| io.insert-koin:koin-test-junit4 | 4.0.0 | Apache-2.0 |
| androidx.test.ext:junit | 1.3.0 | Apache-2.0 |
| androidx.test.espresso:espresso-core | 3.7.0 | Apache-2.0 |
| androidx.compose.ui:ui-test-junit4 *(BOM-managed)* | — | Apache-2.0 |
| androidx.compose.ui:ui-test-manifest *(BOM-managed)* | — | Apache-2.0 |
| androidx.room:room-testing | 2.6.1 | Apache-2.0 |

## License summary

- **Apache-2.0** — all AndroidX/Jetpack/Compose libraries, Google Material, Koin, MockK, Turbine, kotlinx-coroutines, Kotlin, AGP, KSP. The app itself is Apache-2.0 (`LICENSE`, `NOTICE`).
- **EPL-1.0** — JUnit 4 (test-only).
- **JSON License** — `org.json:json` (test-only; the "Good, not Evil" clause does **not** apply to the shipped APK because it is not a runtime dependency).

No copyleft (GPL/LGPL/AGPL) dependency is present in any scope. Formula/chemistry
provenance is documented separately in `THIRD_PARTY_NOTICES.md` and the medication
research report.

## Web (`web/`) build-time devDependencies (not shipped to end users)

Build tooling only (Vite, Vitest, TypeScript, ESLint, vite-plugin-pwa). See
`web/package.json`. The deployed web artifact is static HTML/CSS/JS with no runtime
npm dependency. These versions are mid-upgrade in the working tree and are out of
scope for the Android release SBOM.
