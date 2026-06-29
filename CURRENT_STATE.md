# CURRENT_STATE — autonomous execution handoff

**Branch:** v2.0-wip (local-only, no upstream) · **Latest commit:** `96328b0`

## Commit policy (standing)
- **No `Co-Authored-By` trailer.** All commits under **GurucharanSavanth <savanthgc@gmail.com>** only.
- v2.0-wip history was rewritten (filter-branch) to strip the old Claude co-author trailer from all 34 commits;
  content verified byte-identical. Hashes below the rewrite point changed.

## History v2 — COMPLETE & device-verified
- **Precision:** `StoredDecimal` (canonical String, no Double for new values; legacy binary64 path quarantined).
- **Registries:** event types, precision status, units (+ legacy engine measures), routes, parameter types.
- **Schema:** append-only 3-table model (history_event + dose/parameter_event_detail), AppDatabase **v2**,
  non-destructive `Migration(1,2)` (defensive legacy conversion; orphan v1 vertical retired). Exported `2.json`.
- **Write path:** typed commands + `HistoryWriteValidator` + `HistoryEventRepository` (idempotent, atomic) +
  `LogAdministeredDoseUseCase` / `RecordWaterParameterReadingUseCase`.
- **Read screen:** `HistoryViewModel` (Koin/StateFlow) + Compose `HistoryScreen` (filters, detail, voided badge,
  loading/empty/error/retry, a11y), top-bar History action.
- **Write triggers (both wired):** dashboard **Save readings**; calculator **Log as administered** (logs physical
  g/mL amount — spoons never logged as exact).
- **Gates:** JVM **142/0/0**, instrumented **16/0** on emulator-5554, lint clean, Koin verify, tree clean.
- **Release gate (green):** `assembleRelease` builds — R8 minify + resource shrink + lintVital all pass, **no proguard
  keep rules needed** (Koin/Room/Compose history code survives shrinking). Signed 17M APK at
  `app/build/outputs/apk/release/SeachemDosing-v1.0-release.apk`.

## Device-test blocker (recorded, non-fatal)
- Compose UI tests (`createComposeRule`/Espresso) FAIL on emulator-5554 (API-"17" preview): `NoSuchMethodException:
  InputManager.getInstance` (espresso 3.6.1 gap). Non-Espresso instrumented tests unaffected. Revisit with a
  stable API 34/35 AVD or espresso 3.7. HistoryScreen states covered by VM + integration tests.

## Remaining — owner-gated (autonomous bounded work done)
Bounded autonomous items complete (History v2, orphan-code removal, security/licensing docs, theme tokens, dead
resources, release gate). What's left needs an owner decision or an environment fix — not safe to default:
- **LICENSE** — none present; legal choice (MIT/Apache-2.0/proprietary) is the owner's. Blocks public distribution.
- **god `MainViewModel` → StateFlow refactor** — large, behavior-risk; Compose-test coverage blocked (see below),
  so regressions are hard to gate. Do under a stable AVD.
- **Kannada (KN) parity** for new History/medication/fertilizer strings — needs a native speaker for quality.
- **Compose UI tests** — blocked by emulator-5554 Espresso/InputManager gap; needs API 34/35 AVD or espresso 3.7.
