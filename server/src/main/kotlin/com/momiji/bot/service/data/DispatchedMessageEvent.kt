package com.momiji.bot.service.data

import com.momiji.bot.repository.entity.ChatEntity
import com.momiji.bot.repository.entity.UserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType

data class DispatchedMessageEvent(
    val user: UserEntity,
    val chat: ChatEntity,
    val message: DispatchedMessage,
    val command: MessageCommand?,
    val isUpdated: Boolean,
    val frontend: String,
)

data class DispatchedMessage(
    var id: Long? = null,
    var text: String? = null,
    var s3Bucket: String? = null,
    var s3Key: String? = null,
    var mediaType: MediaType? = null,
    var user: UserEntity,
    var replyTo: DispatchedRepliedMessage? = null,
    var nativeId: String,
)

data class DispatchedRepliedMessage(
    var id: Long? = null,
    var text: String? = null,
    var s3Bucket: String? = null,
    var s3Key: String? = null,
    var mediaType: MediaType? = null,
    var user: UserEntity,
    var replyToNativeId: String? = null,
    var nativeId: String,
)
