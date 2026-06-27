package com.example.seachem_dosing.ai

data class AiInsightState(
    val isLoading: Boolean = false,
    val text: String? = null,
    val error: String? = null
)

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AiMessage(
    val role: ChatRole,
    val text: String
)
