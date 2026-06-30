# Software Bill of Materials (SBOM)

**Release artifact:** `app/build/outputs/apk/release/SeachemDosing-v2.0-release.apk`
**Machine-readable artifact:** `build/reports/sbom/bom.cdx.json`
**Format:** CycloneDX 1.6 JSON
**Scope:** declared direct Android dependencies + direct web devDependencies.

## Generate

```bash
node scripts/generate-sbom.js
```

The generator reads:

- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `web/package-lock.json`

It validates the minimum CycloneDX structure before writing. The JSON artifact is
not committed; `.github/workflows/sbom.yml` uploads it as `sbom-cyclonedx`.

## Policy

This project keeps SBOM instructions in Git and publishes generated SBOM JSON as
a CI/release artifact. The app is offline and no-backend; direct dependency
coverage is the current release evidence. Use a full CycloneDX Gradle/npm setup
only if release requirements need transitive dependency evidence.
