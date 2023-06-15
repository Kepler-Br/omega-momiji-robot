package com.momiji.bot.repository.entity

import com.momiji.bot.repository.entity.enumerator.MediaType
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table


@Table(
    name = "messages",
    schema = "gateway",
)
data class MessageEntity(
    @Id
    var id: Long? = null,
    var text: String? = null,
    var s3bucket: String? = null,
    var s3key: String? = null,
    var mediaType: MediaType? = null,
    var chatId: Long? = null,
    var userId: Long? = null,
    var createdAt: LocalDateTime? = null,
    var replyToMessageNativeId: String? = null,
    var frontend: String,
    var nativeId: String,
)
