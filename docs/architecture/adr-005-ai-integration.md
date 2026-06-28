# ADR-005: AI Integration — Hybrid (Gemini API + Optional Local LLM)

**Status:** Superseded by [ADR-010](adr-010-remove-ai-chat.md) (2026-06-28 — AI/chat code removed; this ADR retained as historical design analysis and the basis for any future re-entry).
**Date:** 2026-05-09
**Deciders:** Gurucharan.S
**User decision:** B (Local LLM) → revised to **5c (Hybrid)** after device-class reality check

## Context

Current state:
- `ai/GeminiClient.kt` is fully stubbed (`isConfigured() = false`, every method returns `Result.failure`).
- `ai/AiModels.kt` has dual enums (`ChatRole` + `AiRole`) for chat messages — duplication smell.
- `MainViewModel` exposes `aiInsight: LiveData<AiInsightState>` and `chatMessages: LiveData<List<ChatMessage>>` — UI-ready, backend-empty.
- `app/build.gradle.kts` has `applicationId="com.example.seachem_dosing"`, `minSdk = 24` (Android 7.0).
- No INTERNET permission yet in `AndroidManifest.xml`.

The user's initial pick was Option B (Local LLM via ONNX/ML Kit). Reality check:
- **ML Kit GenAI** requires AICore = Android 14+ on Pixel 8+ / Galaxy S24+ only. Hard fail on most install base of `minSdk 24`.
- **ONNX Runtime + Gemma-2B/Phi-mini** = 1.5–4GB model + 2–4GB RAM working set. Android 7-era devices typically have 2GB total RAM. Either model unloadable.
- **MediaPipe LLM Inference + Gemma-2-2B-IT-Q4** = ~1.5GB model download, ~2GB RAM working set, requires SDK 26+ (Android 8.0).

User revised pick to **5c (Hybrid)**: Gemini API as default for all SDK 24+; opt-in MediaPipe LLM for SDK 26+ devices with sufficient RAM.

## Decision

**Hybrid AI integration:**

1. **Default path** — Gemini API (cloud, online-only). Available to all `minSdk 24+` devices. User provides API key via `Settings`. No telemetry beyond Google's terms.
2. **Opt-in path** — MediaPipe LLM Inference with Gemma-2-2B-IT INT4-quantized. Available only when:
   - `Build.VERSION.SDK_INT >= 26` (Android 8.0+), AND
   - `ActivityManager.getMemoryInfo().totalMem >= 3 * 1024L * 1024L * 1024L` (3GB+ device RAM), AND
   - User explicitly enables in `Settings → AI → Use local model` toggle.
3. **Routing** — `AiRepository.generate(prompt)` checks settings + device capability and dispatches to `GeminiClient` or `LocalLlmClient`. Falls back to Gemini if local unavailable but enabled.

## Options Considered (revised post reality check)

### Option 5a — Gemini API only
Simplest. All devices. Online-only. Needs API key.

| Dimension | Score (1-5) |
|---|---|
| Latency | 4 (network) |
| Cost | 2 (per-token billing) |
| Privacy | 2 (cloud) |
| Capability | 5 (Gemini 1.5 Flash / Pro) |
| Offline support | 1 |
| Device coverage | 5 (all SDK 24+) |

### Option 5b — Local LLM only (MediaPipe)
Requires SDK 26+ + 3GB RAM. Excludes older devices entirely.

| Dimension | Score (1-5) |
|---|---|
| Latency | 5 (local) |
| Cost | 5 (free) |
| Privacy | 5 (no data leaves device) |
| Capability | 3 (Gemma-2B is small) |
| Offline support | 5 |
| Device coverage | 2 (excludes ~30% of install base) |

### Option 5c — Hybrid (CHOSEN)
Both. Gemini default; local opt-in for capable devices.

| Dimension | Score (1-5) |
|---|---|
| Latency | 4-5 |
| Cost | 3 (Gemini for most, free for opt-in users) |
| Privacy | 3-5 (depends on choice) |
| Capability | 4 |
| Offline support | 3 (local users only) |
| Device coverage | 5 (everyone gets Gemini at minimum) |

**Pros:** Universal access (Gemini works for all). Privacy/offline path for capable devices. No exclusion.
**Cons:** Two clients to maintain. Settings UX must explain capability gating clearly.

## Implementation Plan

### Build Configuration

```kotlin
// app/build.gradle.kts
android {
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // ...
        buildConfigField("boolean", "AI_LOCAL_LLM_AVAILABLE", "true")
    }
}
```

No build variants needed — gating happens at runtime via `Build.VERSION.SDK_INT` + memory check + settings toggle.

### Dependencies

```kotlin
// app/build.gradle.kts (dependencies)
// Gemini
implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

// MediaPipe Local LLM (optional path)
implementation("com.google.mediapipe:tasks-genai:0.20.0") // SDK 26+ runtime

// Network
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

### Manifest

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### AI Layer

```kotlin
// ai/AiClient.kt — interface
interface AiClient {
    suspend fun generate(messages: List<AiMessage>, systemPrompt: String): Result<String>
    fun isAvailable(): Boolean
}

// ai/GeminiClient.kt — full impl (replaces current stub)
class GeminiClient(
    private val apiKey: String?,
    private val model: String = "gemini-1.5-flash"
) : AiClient {
    override fun isAvailable() = !apiKey.isNullOrBlank()
    override suspend fun generate(...) { /* via google.ai.client.generativeai */ }
}

// ai/LocalLlmClient.kt — new
@RequiresApi(Build.VERSION_CODES.O) // SDK 26+
class LocalLlmClient(
    private val context: Context,
    private val modelPath: String
) : AiClient {
    override fun isAvailable() = File(modelPath).exists() && hasEnoughMemory()
    override suspend fun generate(...) { /* via mediapipe tasks-genai */ }
    private fun hasEnoughMemory(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem >= 3L * 1024 * 1024 * 1024
    }
}

// ai/AiRepository.kt — routing
class AiRepository(
    private val gemini: GeminiClient,
    private val local: LocalLlmClient?,
    private val settings: SettingsRepository
) {
    suspend fun generate(prompt: String): Result<String> {
        val useLocal = settings.useLocalLlmEnabled.first() &&
                       Build.VERSION.SDK_INT >= 26 &&
                       local?.isAvailable() == true
        return if (useLocal && local != null) {
            local.generate(...).recoverCatching { gemini.generate(...).getOrThrow() }
        } else {
            gemini.generate(...)
        }
    }
}
```

### Settings UX

`Settings → AI`:
- "Gemini API Key" (text input, secure storage in EncryptedSharedPreferences)
- "Use local model on device" (toggle, disabled with explainer if SDK < 26 or RAM < 3GB)
- "Download Gemma-2B-Q4 model (1.5GB)" (button, shown when toggle enabled but model not present)

### Privacy

- Gemini API: opt-in via key entry. No automatic telemetry.
- Local LLM: opt-in via toggle. Model downloaded over HTTPS from a configured CDN. No cloud calls during inference.
- Aquarium parameter context is sent to whichever model the user picked. Document in privacy policy.

### Cost

- Gemini API: Google's per-token pricing. Most aquarium queries are <500 tokens → ~$0.0002/query for Gemini 1.5 Flash. Acceptable.
- Local LLM: zero per-query cost. One-time model download (1.5GB).

## Consequences

**Easier:**
- Universal access — every user has at least Gemini.
- Privacy-conscious users with capable devices get full offline.
- Adding a third AI provider (e.g., Claude API) is just another `AiClient` implementation.

**Harder:**
- Two client codepaths + integration tests for each.
- Model download UX (1.5GB) needs progress UI, retry, integrity check.
- Settings UX must explain device gating without confusion.

**Revisit if:**
- MediaPipe LLM SDK matures to support SDK 24 → drop SDK gating.
- Model size shrinks (Gemma-2-Q4 → Gemma-1B-Q4) → relax RAM gating.
- Gemini pricing changes materially → reconsider default.

## Action Items

1. Add Gemini + MediaPipe deps to `gradle/libs.versions.toml`.
2. Add `INTERNET` + `ACCESS_NETWORK_STATE` permissions to `AndroidManifest.xml`.
3. Replace `ai/GeminiClient.kt` stub with full implementation using `com.google.ai.client.generativeai`.
4. Create `ai/AiClient.kt` interface, `ai/LocalLlmClient.kt`, `data/repository/AiRepository.kt`.
5. Resolve `AiModels.kt` enum duplication: keep `ChatRole`, delete `AiRole`, update `AiMessage` to use `ChatRole`.
6. Add Settings UX (Compose: `AiSettingsScreen`) — API key entry + local toggle + download progress.
7. EncryptedSharedPreferences for API key storage (`androidx.security:security-crypto:1.1.0-alpha06`).
8. Wire AiRepository into Koin `aiModule` (per ADR-002), feature-flagged via `BuildConfig.AI_LOCAL_LLM_AVAILABLE`.
9. Integration tests with mocked AiClient for `MainViewModel.aiInsight` flow.
10. Privacy policy update — disclose Gemini API usage + local model download URL.

## Rollback Criteria

Revert to Option 5a (Gemini-only) if:
- MediaPipe `tasks-genai` proves unstable on supported devices (>10% crash rate).
- Model download CDN bandwidth costs unsustainable.
- User feedback shows the gating UX is confusing (>3 support requests).

Revert to Option 5b (local-only, raise minSdk to 26) only if Gemini cost grows unsustainable AND device-coverage tradeoff is acceptable.
