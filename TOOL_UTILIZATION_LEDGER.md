# TOOL_UTILIZATION_LEDGER.md

**Project:** Seachem-Dosing → aquarium super-app modernization (v2.0)
**Last updated:** 2026-06-27
**Source of inventory:** live Claude Code session (`/plugin list` output + session skill/MCP enumeration), **not** the prompt's expected inventory. Where the two disagree, the live environment wins and the delta is recorded here + in `TOOL_FAILURE_LOG.md`.

> A tool is "handled" when it is **used** (with recorded output), **not-applicable** (concrete reason), **blocked** (auth/connection/permission), or **deferred** (behind a gate). Relevance + least-privilege + privacy outrank raw invocation count. No invocation, result, capability, or version below is fabricated.

---

## 1. Plugins (live: 10 enabled)

| name | source | version | live status | relevance | phase | result so far | notes / nonuse reason |
|---|---|---|---|---|---|---|---|
| caveman | caveman | v18e453 | enabled | High — terse comms, compress, review, commit, stats | all | **in use** | Caveman prose mode active (session hook). `caveman-commit/review/stats` planned P9–P10. |
| ck | cavekit-marketplace | 4.0.0 | enabled | High — SDD: `spec`/`build`/`check`/`backprop` | P2–P10 | deferred→active | `ck:spec` etc. are the same SDD skills as bare `/spec`. Will drive SPEC.md + §V invariants. |
| code-review | claude-plugins-official | — | enabled | High — diff/PR correctness review | P4, P9 | deferred | Run after each significant batch + before acceptance. |
| hookify | claude-plugins-official | — | enabled | Med — repo guardrails (pre-commit secret/test gate) | P9 | deferred (owner-approval) | Propose reversible hooks; will not install without showing config + rollback. |
| ponytail | ponytail | 4.8.3 | enabled | High — over-engineering governor | all | **in use (full)** | Set to `full` this session. Keeps deliverables citing existing work, not duplicating it. |
| ralph-loop | claude-plugins-official | 1.0.0 | enabled | Med — bounded repair loops | P6–P7 | deferred | Only with task def + max-iters + test + stop condition. No unbounded loops. |
| sap-mdk-server | claude-plugins-official | 0.4.0 | **enabled** | **Not applicable** | — | n/a | Native Android + vanilla/TS web; no SAP MDK. **Corrects prompt** (which expected `mdk-mcp` "failed"). |
| security-guidance | claude-plugins-official | 2.0.6 | enabled | High — threat model, deps, storage, signing | P4, P9 | deferred | Medication/secret/Firebase/release surfaces. |
| supabase | claude-plugins-official | 0.1.11 | enabled | **Not applicable** | — | n/a | Offline-first app; no Supabase in repo or ADR-001..005. Will not create projects/keys. |
| superpowers | claude-plugins-official | 6.0.3 | enabled | High — brainstorming, **writing-plans (=/plan fallback)**, TDD, debugging, verification | P2–P10 | **in use** | `using-superpowers` active. `writing-plans` substitutes for missing `/plan`. |

**Expected-but-absent (see TOOL_FAILURE_LOG.md):**
- `agent-skills` (addy-agent-skills) — **not installed** (prompt expected "failed_to_load"). Nothing to repair; needs owner install.
- `mdk-mcp` — no such plugin; the SAP MDK capability is `sap-mdk-server`, **enabled** above.

---

## 2. Skills (user-invocable, key subset)

| skill | phase | result | invocation / fallback |
|---|---|---|---|
| `deep-research` | P1 | **next** | Scoped to genuine gaps only (API-33→36 behavior, medication evidence, fertilizer chemistry). |
| `spec` / `ck:spec` | P2 | deferred | → SPEC.md with §V calculation/safety invariants. |
| `build` / `ck:build` | P3+ | deferred | Plan-then-execute per phase. |
| `check` / `ck:check` | gates | deferred | Drift audit at each phase gate. |
| `backprop` / `ck:backprop` | on-failure | deferred | Bug→§V invariant after substantive failures. |
| `superpowers:writing-plans` | P2 | deferred | **Explicit `/plan` substitute** — `/plan` not present in this env. |
| `superpowers:test-driven-development` | P3,P6,P7 | deferred | Engine + safety-rule tests before impl. |
| `superpowers:systematic-debugging` | on-failure | deferred | Before any fix to baseline/build failures. |
| `superpowers:verification-before-completion` | gates | deferred | Evidence before "done" claims. |
| `cavecrew` (investigator/builder/reviewer) | P1+ | deferred | Bounded, non-overlapping subagent tasks; compressed output. |
| `caveman-review` | reviews | deferred | Reconcile with `code-review` plugin. |
| `caveman-commit` | P10 | deferred | Atomic messages, reviewed passing batches only. No push. |
| `caveman-stats` | milestones | deferred | Operational metadata, not a quality proof. |
| `caveman-compress` | context | deferred | Only if context nears limit; never compress away safety records. |
| `caveman-help` | as-needed | deferred | When command semantics uncertain. |

---

## 3. MCP servers

| MCP | live status | relevance | result | reason |
|---|---|---|---|---|
| Mermaid Chart | connected | High — DIAGRAMS/ (arch, nav, safety gates, calc flows) | deferred → P2/P9 | Will store diagram source in repo. |
| Scholar Gateway | connected | Med — aquaculture/chemistry literature | deferred → P1 | Verify metadata vs primary sources. |
| Figma | connected | Med — UI inventory/wireframes | deferred (owner-auth) | Only if you authorize; not medication/chem evidence. |
| Hugging Face | connected (auth: Savanthgc) | **Not applicable** | n/a | No on-device model approved; AI is stubbed (ADR-005). No generative medication advice. |
| freecad | connected | **Not applicable** | n/a | No CAD requirement. |
| ui5-mcp-server | connected | **Not applicable** | n/a | Web app is vanilla JS + a TS/Vite rewrite, not SAP UI5. |
| Google Drive | auth-required | None now | blocked-by-authorization | Won't authenticate without an explicit file task. |
| Gmail | auth-required | None | not-required | No email task. Won't authenticate. |
| Google Calendar | auth-required | None | not-required | No scheduling task. Won't authenticate. |
| Candid / Synapse.org | auth-required | None | not-required | No approved dataset need. |
| Notion | connected | None now | not-applicable | Repo Markdown is canonical record. |
| Coupler.io / Explorium / LunarCrush | connected | None | not-applicable | No data-import/business/market-analytics scope. Never medication/chem evidence. |
| (Canva, Mercury, Era Context, Windsor.ai, Groww) | auth-required | None | not-applicable | Out of scope. |

---

## 4. Agents & Hooks

| type | name | relevance | result |
|---|---|---|---|
| agent | `Explore` | repo mapping | may use (read-only sweeps) |
| agent | `Plan` | plan design | candidate `/plan` alt alongside writing-plans |
| agent | `caveman:cavecrew-investigator` | locate code | bounded delegation P1+ |
| agent | `caveman:cavecrew-builder` | 1–2 file edits | bounded delegation |
| agent | `caveman:cavecrew-reviewer` | diff review | reviews |
| agent | `general-purpose` / `claude` | catch-all | as needed |
| agent | statusline-setup, claude-code-guide, hookify:conversation-analyzer | — | not applicable |
| hook | caveman mode tracker (SessionStart/UserPromptSubmit) | comms mode | **active** (informational) |
| hook | ponytail mode (SessionStart/UserPromptSubmit) | laziness governor | **active** (informational) |
| hook | repo guardrails (pre-commit secret/test) | safety | **none configured** → hookify proposal deferred P9 |

---

## 5. Completeness statement

Every expected plugin/skill/MCP from the prompt is accounted for above as used / not-applicable / blocked / deferred. **Complete utilization is NOT yet claimed** — rows marked "deferred" will be updated with real invocation + output as each phase executes. Owner-approval gates: Figma auth, hookify install, any push/release/signing, any Supabase/Drive/Gmail use.
