package com.momiji.bot.service.neuro

data class ChatGenerationConfig(
    val fullname: String,
    val maxNewTokens: Int,
    val numBeams: Int,
    val noRepeatNgramSize: Int,
    val temperature: Float,
    val topP: Float,
    val topK: Int,
    val repetitionPenalty: Float,
)
