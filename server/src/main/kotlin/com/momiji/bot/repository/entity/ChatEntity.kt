package com.momiji.bot.repository.entity

import com.momiji.bot.repository.entity.enumerator.ChatType
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table(
    name = "chats",
    schema = "gateway",
)
data class ChatEntity(
    @Id
    var id: Long? = null,
    var createdAt: LocalDateTime? = null,
    var title: String,
    var frontend: String,
    var nativeId: String,
    var type: ChatType,
)
