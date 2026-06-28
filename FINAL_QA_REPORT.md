# FINAL_QA_REPORT.md

**Project:** Seachem-Dosing v2.0 · **Branch:** `v2.0-wip` · **Date:** 2026-06-28 · **Base:** master `f6df401` (untouched)

## Build / test / lint

| Gate | Result |
|---|---|
| `assembleDebug` | ✅ SUCCESSFUL |
| `testDebugUnitTest` | ✅ **54 / 54 green** (0 fail/skip/error) |
| `lintDebug` | ✅ **0 errors**, 204 warnings, 7 hints |
| On-device smoke (Pixel_10_Pro_XL API 36) | ✅ all 5 screens render + key interactions (see TESTING_STRATEGY) |

### Lint warning breakdown (no errors)
- 133 `UnusedResources` — leftover strings/resources from the deleted XML screens. Non-blocking; deeper string sweep is follow-up.
- 22 `GradleDependency` + 10 `NewerVersionAvailable` + 9 `UseTomlInstead` — dependency-update suggestions.
- 7 `DefaultLocale` (`String.format` without Locale), 3 `HardcodedText` (medication/fertilizer literal labels — localization debt, noted), 3 `ObsoleteSdkInt` (dead `SDK_INT` checks at minSdk 33), 6 `AutoboxingStateCreation`, misc.

## Security self-review (security-guidance substance)

| Surface | Finding |
|---|---|
| Secrets | `app/google-services.json` (Firebase key) **gitignored**, never committed — verified by a secret-staging guard on every commit. No API keys in source. |
| AI keys | `GeminiClient` stubbed (`isConfigured()`=false); no key wired. |
| Signing | Release signing via gradle/env properties (`SEACHEM_RELEASE_*`); no keystore in repo. **Untouched.** |
| Input validation | `ValidationEngine` at the calc boundary + ViewModel `coerce*` (non-negative, pH 0–14, %, salinity) + sealed `CalcResult` — bad input → typed result, never a crash. |
| Medication safety | Hard gates (§V4–V6): no dose without confirmed water-type/volume/inverts/filtration/species; FW/SW & reef mismatches blocked; no invented facts. |
| Network / data | Offline-first, no backend. Export = clipboard (local). Share = system chooser (user-initiated). No silent exfiltration. |
| Platform | minSdk 33 (Android 13+) modern security baseline; `isMinifyEnabled`/`isShrinkResources` on release. |

**Recommended (owner-triggered, heavy):** run the `security-guidance` and `code-review` plugins for a deeper automated pass over the full branch diff. Not run here (branch-wide diff is large; this self-review covers the substance).

## Known limitations / open items

1. **Instrumented Compose tests blocked** on the API-36 emulator (Espresso `InputManager.getInstance`, F-07). Verified via screenshot+tap instead. Fix: API ≤ 34 AVD or espresso 3.7.x.
2. **Web research pending (task #14)** — 5 non-Seachem med doses are SECONDARY; Kordon Rid-Ich Plus aquarium dose **unverified** (`doseVerified=false`, UI warns). Re-fetch when WebFetch rate-limit resets.
3. **Localization debt** — Medication/Fertilizer module labels are English literals (not yet in `strings.xml`/`values-kn`).
4. **133 unused-resource warnings** — leftover strings from removed XML; cosmetic sweep pending.
5. **R6 dual web stack** (`Base_Template/` vs `web/`) — owner decision pending; Android unaffected.
6. **Volume editing** moved to Dashboard (removed from Calculators) — intentional scope decision.

## Acceptance criteria (from MODERNIZATION_PLAN §13)

- ✅ build passes · ✅ 54 tests pass · ✅ 0 lint errors
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
