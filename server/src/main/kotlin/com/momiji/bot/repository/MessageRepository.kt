package com.momiji.bot.repository

import com.momiji.bot.repository.entity.MessageEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface MessageRepository : CrudRepository<MessageEntity, Long> {
    fun getByFrontendAndNativeId(frontend: String, nativeId: String): MessageEntity
}
