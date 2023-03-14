package com.momiji.bot.repository

import com.momiji.bot.repository.entity.ChatEntity
import org.springframework.data.repository.CrudRepository

interface ChatRepository : CrudRepository<ChatEntity, Long> {
    fun getByFrontendAndNativeId(frontend: String, nativeId: String): ChatEntity
}
