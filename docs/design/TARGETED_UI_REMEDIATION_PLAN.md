# Targeted UI Remediation Plan — v2.0-wip

Commit `1d0744f`. **No code changed yet — this is the proposal to approve before any modification.** Scope rule honoured: functioning screens are not redesigned for novelty; most are *retain + correct*.

## Design tooling — honest status
- **Claude Design plugin: not available** in this environment. Available design surfaces: **Figma MCP** (mockups/handoff) + **Mermaid** (diagrams, already in repo `DIAGRAMS/`). `/design-login` reported "authorized" but no Claude Design tool is present to invoke.
- Therefore every proposal here is **code-grounded manual analysis**. Where visual mockups would help (adaptive nav, result card, warning panel), they can be produced in Figma on request and linked — not fabricated.

## Per-screen classification

| Screen | Classification | Why |
|---|---|---|
| Settings | **retain unchanged** | thin; uses VM export/reset; no issues found |
| Profile | retain + interaction/a11y correction | full read pending; entry flow OK |
| Dashboard | retain + token correction | works via engine-backed VM; needs design-system tokens + theme-propagation verify |
| Calculators | **refactor** | drives the 26-arm VM `when`; route through use cases + own VM |
| Medication | retain + a11y + state correction | logic is the safety-critical strength; fix volatile state, semantics, i18n |
| Fertilizer | retain + a11y + state correction | same pattern as Medication |
| (History) | **complete missing functionality OR remove** | Room+repo built, no screen (ISSUE-CONN-003) — owner decision |
| (AI/chat) | **remove after confirm** | inert stub + orphan resources (ISSUE-CONN-004) |

## Sequence (shell + design system FIRST, per instruction)

**Step 0 — approval gate (now).** Owner approves this plan + the History/AI/usecase-layer decisions below. No code until then.

**Step 1 — Design-system foundation** (`:core:designsystem` or `ui/theme` + `ui/components`)
- Single color source (kill the `Color.kt`↔`colors.xml` duplication, ISSUE-DS-001) — pick Compose-as-source, generate/replace XML or drop XML theme once shell is Compose.
- Add spacing / shape / motion tokens (DS-004/005/006); real type scale (DS-003).
- Extract shared components from the 4× duplicates: `AppDropdown`, `AppButton`, `WarningPanel` (heading/liveRegion), `ResultCard` (renders `CalcResult`), `StatusBadge` (text+icon, not color-alone), `SwitchRow`, `SectionHeader(heading())`. Closes DS-007, A11Y-001/002.
- Tests: screenshot/golden per component (light/dark/font-scale); semantics tests.

**Step 2 — App shell + adaptive nav**
- Decide ARCH-001: keep Fragment shell (and **remove `navigation-compose`**) or migrate to single-Activity Compose `NavigationSuiteScaffold`.
- Add `WindowSizeClass` → bottom bar (compact) / rail (medium) / rail+pane (expanded); content `widthIn(max)` wrapper (RESP-001).
- Verify theme-mode propagation to Compose on device (DS-008, test T-THEME-1).
- Global aquarium-profile context, beginner/expert toggle pattern, global search entry — design as shell affordances.

**Step 3 — State + domain wiring** (per screen, behind tests)
- Introduce feature ViewModels; move engine calls out of composables (ARCH-002/003).
- `rememberSaveable`/VM for all flow state (STATE-001).
- Route dosing through the **existing** use cases (CONN-002); decide History (CONN-003).
- i18n extraction for medication/fertilizer (I18N-001).

**Step 4 — Cleanup (only after parity)**
- Remove dead VM holders, AI/chat orphans, unused resources (ARCH-004, CONN-004) after `lint UnusedResources` + grep confirm.

## Decisions — RESOLVED 2026-06-28 (owner)
1. **Shell:** Keep Fragment/XML shell this phase; do not migrate to Compose `NavHost`. → [ADR-007](../architecture/adr-007-retain-fragment-shell.md). `navigation-compose` removal gated on a repo-wide dependency-analysis proof (separate follow-up commit).
2. **History feature:** Build & connect the full History vertical slice (use the existing Room layer). → [ADR-008](../architecture/adr-008-history-feature.md).
3. **AI/chat:** Remove the complete orphan implementation (incl. the GeminiClient stub) after reachability proof; preserve future-AI requirements in docs. → [ADR-010](../architecture/adr-010-remove-ai-chat.md) (supersedes ADR-005).
4. **Colour tokens:** Compose-first canonical semantic tokens + controlled XML compatibility bridge while the shell stays XML. → [ADR-009](../architecture/adr-009-colour-tokens.md).

## Owner-mandated execution order
1. Record the four decisions in ADRs + this plan. ✅ (this commit)
2. Verify + commit the pending `menuAnchor` fixes separately. ✅ (`1d0744f`)
3. Remove confirmed orphan AI/chat code — isolated commit.
4. Establish the semantic colour-token bridge — isolated commit.
5. Implement the History vertical slice — reviewable commits.
6. Run full build + unit + lint + UI + Room-migration + navigation test suites.
7. Resume remaining repository audit + targeted design remediation.

Rule (owner): no unrelated changes in one commit; no completion claim without command output + test evidence.

## Risks
- Shell migration is the highest-risk item → gate behind design-system completion + screenshot baselines; do screen-by-screen with parity tests; never delete XML until the Compose replacement passes parity (matches the existing project rule).
- Removing the use-case/History/AI layers is irreversible-ish → require grep + lint + owner sign-off (do not delete on suspicion).
- Theme-propagation fix must be device-verified before claiming.

## Acceptance tests (plan-level)
- T-DS-*: component golden tests (light/dark/font-scale/RTL).
- T-THEME-1: forced Light vs system Dark → Compose content matches chosen mode (on device).
- T-STATE-1..n: rotate + process-death preserves each flow's inputs.
- T-RESP-1..3: layout correct at compact/medium/expanded + foldable.
- T-A11Y-*: heading navigation, status label present per `CalcResult`, contrast AA, TalkBack traversal.
- T-MED-1: beginner flow never emits a numeric dose from the `volumeLitres=1` placeholder path.
- Build/lint/test green after each step; `dependency-analysis` shows no unused nav dep.

**Status: awaiting owner approval (Step 0). No production code will change until the 4 decisions above are answered.**
