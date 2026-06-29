# Security Policy

## Reporting a vulnerability

Please report security issues **privately**, not via public issues:

- GitHub: open a private **Security Advisory** on `GurucharanSavanth/Seachem-Dosing`, or
- Email: **savanthgc@gmail.com**

Include reproduction steps and affected version (`versionName` / `versionCode`). Expect an initial
response within a reasonable window; please allow time to fix before public disclosure.

## Security posture (Android app)

Reviewed at v2.0 (offline-first). Attack surface is intentionally minimal:

| Area | Status |
|---|---|
| Permissions | **None** declared — no `INTERNET`, location, storage, contacts, etc. |
| Network | **None** — the app makes no network calls (the inert AI/chat stub was removed). |
| Exported components | **Only** `MainActivity` (the launcher) is exported. No exported services / receivers / providers, no deep links, no `FileProvider`, no `WebView`. |
| Backup | `allowBackup="false"` — local data is excluded from device/cloud backup. |
| Release build | Release config enables `isMinifyEnabled` + `isShrinkResources` + R8; verify with a fresh `assembleRelease` before release. |
| Signing | Release credentials resolved from Gradle properties / environment variables only — **no keystore or password in source**. `local.properties` is git-ignored. |
| Secrets | No known API keys, tokens, or private keys in tracked source. `app/google-services.json` is gitignored local config; no automated secret scan is currently configured. |

## Data handling

The app stores data on-device only. Current storage is Room v2 for history/audit
records, SharedPreferences for the selected profile, and SavedStateHandle for active
screen state; DataStore Preferences is staged but not wired. This is **low-sensitivity hobbyist data** — no personal
identifiers, credentials, payment data, or human medical records.

- **Not encrypted at rest.** Justified: there are no secrets to protect, and the data is
  low-sensitivity; the threat of a rooted-device attacker reading aquarium readings is out of scope.
  If sensitive data is ever added, adopt Android Keystore-backed encryption (SQLCipher / Jetpack
  Security) and re-evaluate.
- **Append-only audit history** (ADR-011): records are corrected/voided by appending new events,
  never silently mutated; no physical delete is exposed.
- **No telemetry / analytics / crash reporting / advertising.**

## Threat notes (STRIDE-lite)

- **Spoofing / Elevation:** no auth surface; only the launcher activity is exported.
- **Tampering:** local DB is modifiable on a rooted device — accepted (no integrity requirement for
  hobby data; no security decision depends on it).
- **Information disclosure:** no network egress, no PII, backup disabled.
- **Denial of service:** local-only; no remote inputs.
- **Repudiation:** history is append-only with event timestamps + app/engine versions.

## Scope & limitations

This app provides **decision support**, not veterinary diagnosis. Medication/dosing guidance is
evidence-grounded with prominent warnings; see in-app disclaimers. Calculations use precision-safe
arithmetic (`StoredDecimal` / `BigDecimal`); see ADR-011.
