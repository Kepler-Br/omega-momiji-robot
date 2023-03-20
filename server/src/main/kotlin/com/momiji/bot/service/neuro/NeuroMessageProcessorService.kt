package com.momiji.bot.service.neuro

import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.neural.text.TextGenerationController
import com.momiji.api.neural.text.model.HistoryRequest
import com.momiji.api.neural.text.model.MessageType
import com.momiji.bot.repository.ChatRepository
import com.momiji.bot.repository.MessageRepository
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.service.MessageProcessorService
import java.util.UUID
import kotlin.random.Random
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import com.momiji.api.neural.text.model.Message as TextGenerationMessage

class NeuroMessageProcessorService(
    private val messageRepository: MessageRepository,
    private val messageWithUserRepository: MessageWithUserRepository,
    private val chatRepository: ChatRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationController: TextGenerationController,
) : MessageProcessorService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private fun getChatConfig(frontend: String, chatNativeId: String): ChatGenerationConfig {
        return ChatGenerationConfig(
            fullname = "Default",
            maxNewTokens = 100,
            numBeams = 5,
            noRepeatNgramSize = 0,
            temperature = 0.8f,
            topP = 0.95f,
            topK = 50,
            repetitionPenalty = 5.0f,
        )
    }

    private fun maskFullname(fullname: String): String {
        return fullname.hashCode().toString(16)
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
                    throw RuntimeException("Text generation controller returned error: ${response.errorMessage}")
                }

                ResponseStatus.NOT_FOUND -> {
                    throw RuntimeException("Prompt with ID $promptId was not found. This is a bug on text generation service.")
                }
            }
        }

        throw RuntimeException("Cannot get generation result")
    }

    override fun process(request: NewMessageRequest) {
        if (request.isUpdated) {
            return
        }

        val (frontend, chatId, messageId, _) = request

        val chatConfig = getChatConfig(frontend = frontend, chatNativeId = chatId)
        val messages =
            messageWithUserRepository.getByFrontendAndNativeIdAndChatNativeIdOrderByIdDescLimit(
                frontend = frontend,
                nativeId = messageId,
                chatNativeId = chatId,
                limit = contextSize,
            ).map {
                val author = if (it.fullname.trim() == chatConfig.fullname) {
                    maskFullname(it.fullname)
                } else {
                    it.fullname
                }

                TextGenerationMessage(
                    messageType = MessageType.TEXT,
                    // TODO: Ideally, should never be null.
                    content = it.text ?: "Unknown",
                    author = author,
                    messageId = it.nativeId,
                    replyToMessageId = it.replyToMessageNativeId,
                    emoji = null,
                )
            }

        val promptId = UUID.randomUUID()
        val seed = Random.nextLong()

        textGenerationController.generateFromHistory(
            content = HistoryRequest(
                messageType = MessageType.TEXT,
                temperature = chatConfig.temperature,
                maxNewTokens = chatConfig.maxNewTokens,
                numBeams = chatConfig.numBeams,
                repetitionPenalty = chatConfig.repetitionPenalty,
                earlyStopping = true,
                seed = seed,
                topK = chatConfig.topK,
                topP = chatConfig.topP,
                badWords = listOf(
                    " [",
                    "[",
                ),
                history = messages,
            ),
            promptId = promptId,
        )

        val generatedMessages = getGeneratedMessages(promptId)
    }
}
