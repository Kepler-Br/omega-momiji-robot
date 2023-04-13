package com.momiji.bot.service.data

import com.momiji.bot.repository.entity.ChatEntity
import com.momiji.bot.repository.entity.MessageEntity
import com.momiji.bot.repository.entity.UserEntity

data class DispatchedMessage(
    val user: UserEntity,
    val chat: ChatEntity,
    val message: MessageEntity,
    val command: MessageCommand?,
    val isUpdated: Boolean,
    val frontend: String,
)
