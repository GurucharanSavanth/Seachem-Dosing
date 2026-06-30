# Cleanup Execution Ledger

Baseline: branch `v2.0-wip`, start `16650b7cb36a045b733c4c3fd98e4ede2537e3c5`.

## Preflight

| Check | Result |
|---|---|
| `git status --short` | clean |
| `git diff --check` | pass |
| tracked files | 246 |
| tracked lines | 55,543 |
| repo size | 547,348 KiB |
| `./gradlew testDebugUnitTest` | pass |
| `./gradlew lintDebug` | pass |
| `./gradlew assembleDebug` | pass |
| `./gradlew assembleRelease` | pass |
| `node scripts/verify-sync.js` | pass |
| `web`: `npm ci`, typecheck, lint, test, build | pass |

Verbose logs: `build/reports/cleanup/`.

## Candidate Decisions

| Candidate | Current purpose | References found | Decision | Reason | Replacement | Tests required | Commit |
|---|---|---|---|---|---|---|---|
| `existing workflow by claude befor compacting.txt` | Ignored conversation transcript | none | delete | Not tracked; no build/runtime/docs role. | none | `git status`, docs grep | pending |
| `AGENT.md` | Generic AI-agent prompt | self-reference only | delete | Not project architecture or user-facing documentation. | none | `git status`, docs grep | pending |
| `AGENT_WORK_LOG.md` | Agent fan-out notes | none outside transient docs | delete | Duplicates conclusions now captured in research/audit docs. | `DEEP_RESEARCH_REPORT.md` | `git status`, docs grep | pending |
| `CURRENT_STATE.md` | Historical checkpoint summary | audit docs and `PHASE9_VERIFICATION.md` | delete | Stale state doc; current evidence belongs in this ledger. | `docs/audit/CLEANUP_EXECUTION_LEDGER.md` | docs grep | pending |
| `DEEP_RESEARCH_REPORT.md` | Formula, Android, medication evidence | SPEC, ADRs, product schema, code/tests, notices | retain | Durable formula/provenance evidence. | n/a | n/a | n/a |
| `FINAL_QA_REPORT.md` | Final QA/security summary | audit docs | retain | Final QA/security report is explicitly retained evidence. | n/a | n/a | n/a |
| `PHASE9_VERIFICATION.md` | Release/security/parity verification | direct release evidence | retain | Release verification report is explicitly retained evidence. | n/a | n/a | n/a |
| `MODERNIZATION_PLAN.md` | Accepted migration plan | SPEC, phase matrix, final QA | retain | Still referenced as planning/source-of-truth history. | n/a | n/a | n/a |
| `TOOL_FAILURE_LOG.md` | Curated environment/tool failure ledger | deep research, tool ledger, audit docs | retain | Not raw output; documents unresolved/retestable tool constraints. | n/a | n/a | n/a |
| `app/release/` | Tracked generated v1.0 APK, baseline profile `.dm`, output metadata | stale metadata only; docs reference `app/build/outputs/...` for current release output | delete | Generated release outputs belong in build/release artifacts, not source. No tracked key material; history only contains old default Android debug password literals. | `./gradlew assembleRelease` output + CI artifacts | `testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` | pending |
| `docs/sbom/bom.cdx.json` | Hand-authored machine-readable SBOM | docs only | delete | Reproducible generated SBOM now comes from `scripts/generate-sbom.js` and CI upload. | `build/reports/sbom/bom.cdx.json` artifact | `node scripts/generate-sbom.js`, Gradle gates | pending |
