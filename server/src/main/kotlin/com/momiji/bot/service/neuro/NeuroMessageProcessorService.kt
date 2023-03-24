package com.momiji.bot.service.neuro

import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.api.neural.text.TextGenerationController
import com.momiji.api.neural.text.model.GenerationParams
import com.momiji.api.neural.text.model.HistoryRequest
import com.momiji.api.neural.text.model.Message
import com.momiji.api.neural.text.model.MessageType
import com.momiji.bot.repository.ChatGenerationConfigRepository
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.MessageProcessorService
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.Executors
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
class NeuroMessageProcessorService(
    private val messageWithUserRepository: MessageWithUserRepository,
    private val chatGenerationConfigRepository: ChatGenerationConfigRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationController: TextGenerationController,
) : MessageProcessorService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val threadExecutor = Executors.newFixedThreadPool(5)

    override fun process(request: NewMessageRequest) {
        if (request.isUpdated) {
            logger.trace("Not a new message. Skipping.")

            return
        }

        // TODO: Read about propagating TraceID to task. See: LazyTraceThreadPoolTaskExecutor
        threadExecutor.submit {
            try {
                process(
                    frontend = request.frontend,
                    chatId = request.chatId,
                    messageId = request.messageId
                )
            } catch (ex: RuntimeException) {
                logger.error(
                    "Exception has occurred during processing message in thread executor",
                    ex
                )
            }
        }
    }

    private fun process(frontend: String, chatId: String, messageId: String) {
        val chatConfig = getChatConfig(frontend = frontend, chatNativeId = chatId)

        if (Random.nextFloat() > chatConfig.replyChance) {
            logger.trace("Not hitting a replyChance of ${chatConfig.replyChance}. Skipping")

            return
        }

        val messages =
            messageWithUserRepository.getByFrontendAndNativeIdAndChatNativeIdOrderByIdDescLimit(
                frontend = frontend,
                nativeId = messageId,
                chatNativeId = chatId,
                limit = contextSize,
            )
        // TODO: Move message mapper to neural text
        val mapper = MessageMapper()
        val mappedMessages = mapper.mapAndSaveIds(messages, nameToMask = chatConfig.username)

        gatewayMessageSenderController.sendTypingAction(frontend = frontend, chatId = chatId)

        val generatedMessages = generateMessage(messages = mappedMessages, chatConfig = chatConfig)
        val filteredMessages = filterFirstByAuthor(generatedMessages, chatConfig.username)

        for (message in filteredMessages) {
            gatewayMessageSenderController.sendTypingAction(frontend = frontend, chatId = chatId)

            gatewayMessageSenderController.sendText(
                request = SendTextMessageRequest(
                    frontend = frontend,
                    text = message.content,
                    chatId = chatId,
                    replyToMessageId = mapper.mapId(message.replyToMessageId),
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
        val promptId = UUID.randomUUID()
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

        textGenerationController.requestGenerationFromHistory(
            content = HistoryRequest(
                messageType = MessageType.TEXT,
                history = messages,
                generationParams = params,
                promptAuthor = chatConfig.username,
            ),
            promptId = promptId,
        )

        return getGeneratedMessages(promptId)
    }

    private fun getGeneratedMessages(promptId: UUID): List<TextGenerationMessage> {
        val attempts = 100
        val sleep = 300L

        for (i in (0..attempts)) {
            val response = textGenerationController.getGeneratedFromHistory(promptId = promptId)

            return when (response.status) {
                ResponseStatus.OK -> {
                    response.messages
                }

                ResponseStatus.TOO_EARLY -> {
                    Thread.sleep(sleep)
                    continue
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
            }
        }

        throw RuntimeException("Cannot get generation result")
    }

    private fun filterFirstByAuthor(
        generatedMessages: List<Message>,
        author: String,
    ): List<Message> {
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
