package com.momiji.bot.service.neuro

import com.momiji.bot.repository.ChatGenerationConfigRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import java.time.LocalDateTime
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service

@Service
class ChatConfigService(
    private val chatGenerationConfigRepository: ChatGenerationConfigRepository,
) {

    fun save(config: ChatGenerationConfigEntity) {
        chatGenerationConfigRepository.save(config)
    }

    fun getChatConfig(frontend: String, chatNativeId: String): ChatGenerationConfigEntity {
        return try {
            // Best case scenario: config already exists in DB
            chatGenerationConfigRepository.getByFrontendAndChatNativeId(
                frontend = frontend,
                chatNativeId = chatNativeId
            )
        } catch (ex: EmptyResultDataAccessException) {
            try {
                // Good case scenario: config does not exist, and we need to save it
                chatGenerationConfigRepository.save(
                    ChatGenerationConfigEntity(
                        createdAt = LocalDateTime.now(),
                        frontend = frontend,
                        chatNativeId = chatNativeId,
                        username = "Default",
                        maxNewTokens = 100,
                        numBeams = 5,
                        noRepeatNgramSize = 0,
                        temperature = 0.8f,
                        topP = 0.95f,
                        topK = 50,
                        repetitionPenalty = 5.0f,
                        replyChance = 0.2f,
                    )
                )
            } catch (ex: DbActionExecutionException) {
                return when (ex.cause) {
                    // "OK" case scenario: config was somehow already saved in DB by other thread before us.
                    // See "Race condition"
                    // Try to retrieve it
                    is DuplicateKeyException -> {
                        chatGenerationConfigRepository.getByFrontendAndChatNativeId(
                            frontend = frontend,
                            chatNativeId = chatNativeId
                        )
                    }

                    else -> {
                        // Worst case scenario: unexpected error happened
                        throw ex
                    }
                }
            }
        }
    }
}
