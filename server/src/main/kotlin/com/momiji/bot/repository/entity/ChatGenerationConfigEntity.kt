package com.momiji.bot.repository.entity

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(
    name = "chat_configs",
    schema = "bot",
)
data class ChatGenerationConfigEntity(
    @Id
    var id: Long? = null,
    var createdAt: LocalDateTime? = null,
    var frontend: String,
    var chatNativeId: String,

    var username: String,
    var maxNewTokens: Int,
    var numBeams: Int,
    var noRepeatNgramSize: Int,
    var temperature: Float,
    var topP: Float,
    var topK: Int,
    var repetitionPenalty: Float,
    var replyChance: Float,
)
