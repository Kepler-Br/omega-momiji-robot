package com.momiji.bot.repository

import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import org.springframework.data.repository.CrudRepository

interface ChatGenerationConfigRepository : CrudRepository<ChatGenerationConfigEntity, Long> {
    fun getByFrontendAndChatNativeId(
        frontend: String,
        chatNativeId: String
    ): ChatGenerationConfigEntity
}
