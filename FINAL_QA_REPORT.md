# FINAL_QA_REPORT.md

**Project:** Seachem-Dosing v2.0 · **Branch:** `v2.0-wip` · **Updated:** 2026-06-29

This report is a living QA summary. Exact current drift is tracked in
`docs/audit/DOCUMENTATION_TRUTH_AUDIT.md`.

## Build / test / lint

| Gate | Result |
|---|---|
| `assembleDebug` | SUCCESSFUL (2026-06-29) |
| `testDebugUnitTest` | SUCCESSFUL (2026-06-29); generated report: 138 tests, 0 failures, 0 errors, 0 skipped |
| `lintDebug` | SUCCESSFUL (2026-06-29) |
| On-device smoke (Pixel_10_Pro_XL API 36) | Historical adb smoke only; not rerun in this docs-remediation workstream |

### Lint warning breakdown

No fresh warning breakdown is asserted here; inspect the generated lint report for
the exact current list.

## Security self-review (security-guidance substance)

| Surface | Finding |
|---|---|
| Secrets | `app/google-services.json` is gitignored and not part of tracked source. No automated secret pre-commit check is currently configured. |
| AI keys | AI/chat implementation was removed (ADR-010); no AI dependency, API key, or network permission is wired. |
| Signing | Release signing is conditional on `SEACHEM_RELEASE_*` Gradle/env properties; no keystore or password is tracked. |
| Input validation | `ValidationEngine` at the calc boundary + ViewModel `coerce*` (non-negative, pH 0–14, %, salinity) + sealed `CalcResult` — bad input → typed result, never a crash. |
| Medication safety | Hard gates (§V4–V6): no dose without confirmed water-type/volume/inverts/filtration/species; FW/SW & reef mismatches blocked; no invented facts. |
| Network / data | Offline-first, no backend. Export = clipboard (local). Share = system chooser (user-initiated). No silent exfiltration. |
| Platform | minSdk 33 (Android 13+); release build config enables minify/resource shrink when `assembleRelease` is run. |

**Recommended:** run a fresh security scan before release readiness. The self-review above is not a substitute for an automated secret/dependency/MASVS pass.

## Known limitations / open items

1. **Instrumented Compose tests blocked** on the API-36 emulator (Espresso `InputManager.getInstance`, F-07). Verified via screenshot+tap instead. Fix: API ≤ 34 AVD or espresso 3.7.x.
2. **Web research pending (task #14)** — 5 non-Seachem med doses are SECONDARY; Kordon Rid-Ich Plus aquarium dose **unverified** (`doseVerified=false`, UI warns). Re-fetch when WebFetch rate-limit resets.
3. **Localization debt** — Medication/Fertilizer module labels are English literals (not yet in `strings.xml`/`values-kn`).
4. **Documentation drift** — see `docs/audit/DOCUMENTATION_TRUTH_AUDIT.md`.
5. **R6 dual web stack** (`Base_Template/` vs `web/`) — owner decision pending; Android unaffected.
6. **Volume editing** moved to Dashboard (removed from Calculators) — intentional scope decision.

## Acceptance criteria (from MODERNIZATION_PLAN §13)

- Build/lint/unit tests passed in the latest docs-remediation workstream.
- ✅ no unsupported medication/fertilizer claims (all OFFICIAL/SECONDARY/UNKNOWN graded; never auto-filled)
- ✅ no unsafe dose path (sealed-result gates, §V4–V6 tested + on-device)
- ✅ no FW/SW catalog mix-up (§V4 separation tested) · ✅ no duplicate product IDs (§V8 tested)
- ✅ no Compose/XML orphan (dead XML removed, build green) · ✅ master unchanged; no push/release

## Manual review checklist (owner)

- [ ] Run app on a personal device; sanity-check a known dose vs label.
- [ ] Confirm minSdk 33 device-coverage acceptable (~68.9%, ADR-006).
- [ ] Decide R6 (web stack) + localize new module strings.
- [ ] Run `security-guidance` + `code-review` plugins if desired.
- [ ] Verify task #14 medication doses before relying on non-Seachem entries.
