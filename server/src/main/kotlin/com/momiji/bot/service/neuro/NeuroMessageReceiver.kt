package com.momiji.bot.service.neuro

import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.api.neural.text.TextGenerationController
import com.momiji.api.neural.text.model.*
import com.momiji.bot.repository.ChatGenerationConfigRepository
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.DispatchedMessage
import com.momiji.bot.service.MessageReceiver
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service
import com.momiji.api.neural.text.model.Message as TextGenerationMessage


@Service
class NeuroMessageReceiver(
    private val messageWithUserRepository: MessageWithUserRepository,
    private val chatGenerationConfigRepository: ChatGenerationConfigRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationController: TextGenerationController,
) : MessageReceiver {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun process(dispatchedMessage: DispatchedMessage) {
        val chatConfig =
            getChatConfig(
                frontend = dispatchedMessage.frontend,
                chatNativeId = dispatchedMessage.chat.nativeId
            )

        if (Random.nextFloat() > chatConfig.replyChance) {
            logger.trace("Not hitting a replyChance of ${chatConfig.replyChance}. Skipping")

            return
        }

        val messages = messageWithUserRepository.getByFrontendAndChatNativeIdOrderByIdDescLimit(
            frontend = dispatchedMessage.frontend,
            chatNativeId = dispatchedMessage.chat.nativeId,
            limit = contextSize,
        )

        val maskedMessages = messages.map {
            val fullname = if (it.userNativeId == "SELF") {
                chatConfig.username
            } else if (it.fullname == chatConfig.username) {
                it.fullname.hashCode().toString(16).trimStart('-')
            } else {
                it.fullname
            }

            TextGenerationMessage(
                messageType = MessageType.TEXT,
                // TODO: Ideally, should never be null.
                content = it.text ?: "Unknown",
                author = fullname,
                messageId = it.nativeId,
                replyToMessageId = it.replyToMessageNativeId,
            )
        }

        gatewayMessageSenderController.sendTypingAction(
            frontend = dispatchedMessage.frontend,
            chatId = dispatchedMessage.chat.nativeId
        )

        val generatedMessages = generateMessage(messages = maskedMessages, chatConfig = chatConfig)
        val filteredMessages = filterFirstByAuthor(generatedMessages, chatConfig.username)

        for (message in filteredMessages) {
            gatewayMessageSenderController.sendTypingAction(
                frontend = dispatchedMessage.frontend,
                chatId = dispatchedMessage.chat.nativeId
            )

            gatewayMessageSenderController.sendText(
                request = SendTextMessageRequest(
                    frontend = dispatchedMessage.frontend,
                    text = message.content,
                    chatId = dispatchedMessage.chat.nativeId,
                    replyToMessageId = message.replyToMessageId,
                )
            )
        }
    }

    private fun getChatConfig(frontend: String, chatNativeId: String): ChatGenerationConfigEntity {
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

    private fun generateMessage(
        messages: List<TextGenerationMessage>,
        chatConfig: ChatGenerationConfigEntity,
    ): List<TextGenerationMessage> {
        val seed = Random.nextLong()

        val params = GenerationParams(
            temperature = chatConfig.temperature,
            maxNewTokens = chatConfig.maxNewTokens,
            numBeams = chatConfig.numBeams,
            repetitionPenalty = chatConfig.repetitionPenalty,
            earlyStopping = true,
            seed = seed,
            topK = chatConfig.topK,
            topP = chatConfig.topP,
            badWords = listOf(" [", "[", "@", " @"),
        )

        val response = textGenerationController.requestGenerationFromHistory(
            content = HistoryRequest(
                messageType = MessageType.TEXT,
                history = messages,
                generationParams = params,
                promptAuthor = chatConfig.username,
            )
        )

        return getGeneratedMessages(response.taskId!!)
    }

    private fun getGeneratedMessages(promptId: UUID): List<TextGenerationMessage> {
        val response =
            textGenerationController.getGeneratedFromHistory(promptId = promptId, async = false)

        return when (response.status) {
            ResponseStatus.OK -> {
                response.messages!!
            }

            ResponseStatus.BAD_REQUEST -> {
                throw RuntimeException("Text generation controller returned error BAD_REQUEST: ${response.errorMessage}")
            }

            ResponseStatus.INTERNAL_SERVER_ERROR -> {
                throw RuntimeException("Text generation controller returned error INTERNAL_SERVER_ERROR: ${response.errorMessage}")
            }

            ResponseStatus.NOT_FOUND -> {
                throw RuntimeException("Prompt with ID $promptId was not found. This is a bug on text generation service.")
            }

            else ->
                throw RuntimeException("Unexpected response status: ${response.status}")
        }
    }

    private fun filterFirstByAuthor(
        generatedMessages: List<TextGenerationMessage>,
        author: String,
    ): List<TextGenerationMessage> {
        val filteredMessages = mutableListOf<TextGenerationMessage>()

        for (message in generatedMessages) {
            if (message.author == author) {
                filteredMessages.add(message)
            } else {
                break
            }
        }
        return filteredMessages
    }
}