package com.example.seachem_dosing.ai

// Gemini integration disabled for now.
class GeminiClient(
    apiKey: String,
    model: String
) {
    fun isConfigured(): Boolean = false

    fun generateVerified(
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Double = 0.2,
        maxTokens: Int = 512
    ): Result<String> {
        return Result.failure(IllegalStateException("AI disabled for now."))
    }

    fun generate(
        messages: List<AiMessage>,
        systemPrompt: String,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        return Result.failure(IllegalStateException("AI disabled for now."))
    }
}
