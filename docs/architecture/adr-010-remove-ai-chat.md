# ADR-010: Remove orphan AI/chat implementation (re-entry gated)

**Status:** Accepted
**Date:** 2026-06-28
**Deciders:** Gurucharan.S
**Supersedes:** ADR-005 (AI Integration — Hybrid), now marked Superseded. ADR-005 is retained as historical design analysis.

## Context
The AI/chat code is provably unreachable at commit `1d0744f` (reachability battery, 2026-06-28):
- `ai/GeminiClient.kt` = inert stub (`isConfigured()=false`); never instantiated anywhere.
- `ai/AiModels.kt` (`AiInsightState`, `ChatMessage`) referenced only by dead `MainViewModel` LiveData (`_aiInsight`/`_chatMessages`) that **no screen observes**.
- Resources `res/layout/item_chat_message.xml`, `res/drawable/bg_chat_user.xml`, `res/drawable/bg_chat_assistant.xml`, `res/menu/context_recommendations.xml` — zero references.
- 7 `ai_*` strings in `values/strings.xml` (none localized in `values-kn`) — unreferenced.
- **No INTERNET permission, no Gemini/generativeai dependency, no DI binding, no test/mock.**

## Decision
Remove the complete orphan implementation in an isolated commit: `ai/` package, the four orphan resources, the 7 `ai_*` strings, `MainViewModel` chat/insight state + imports + `resetAll` lines, and the stale AI comment in `SeachemDosingApp.kt`. Preserve future AI intent **in documentation** (ADR-005 history + this ADR), not in dead production code.

If any chat component is found reachable mid-removal (a live route/reference/runtime path), **stop and report** the exact route/reference/behaviour instead of deleting.

## Re-entry requirements (must be satisfied before AI returns)
Defined user problem · approved privacy model · provider abstraction (`AiClient` interface) · consent flow · data-retention policy · disclosure (Play Data Safety) · prompt-injection controls · hallucination containment · **deterministic calculator/medication separation** · offline/failure behaviour · cost controls · rate limiting · credential management (no keys in source) · model-output safety validation · **explicit prohibition: model output may never override calculation engines or evidence-backed medication rules.**

## Consequences
- **Easier:** removes dead weight + false "feature exists" signal; smaller surface; no misleading `Gemini Insight` strings.
- **Harder:** re-introducing AI starts from the abstraction in this ADR (not the old stub). ADR-005's analysis remains available.

## Rollback
`git revert` the removal commit restores the stub. ADR-005 retains the full prior design.
