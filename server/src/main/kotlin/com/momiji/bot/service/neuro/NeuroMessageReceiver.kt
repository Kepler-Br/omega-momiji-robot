package com.momiji.bot.service.neuro

import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.api.neural.text.TextGenerationController
import com.momiji.api.neural.text.model.*
import com.momiji.bot.repository.ChatGenerationConfigRepository
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.data.DispatchedMessage
import java.util.UUID
import kotlin.random.Random
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.momiji.api.neural.text.model.Message as TextGenerationMessage


@Service
class NeuroMessageReceiver(
    private val messageWithUserRepository: MessageWithUserRepository,
    private val chatGenerationConfigRepository: ChatGenerationConfigRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
    private val chatConfigService: ChatConfigService,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationController: TextGenerationController,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun process(dispatchedMessage: DispatchedMessage) {
        val chatNativeId = dispatchedMessage.chat.nativeId
        val frontend = dispatchedMessage.frontend

        val chatConfig =
            chatConfigService.getChatConfig(
                frontend = frontend,
                chatNativeId = chatNativeId
            )

        if (Random.nextFloat() > chatConfig.replyChance) {
            logger.trace("Not hitting a replyChance of ${chatConfig.replyChance}. Skipping")

            return
        }

        val messages = messageWithUserRepository.getByFrontendAndChatNativeIdOrderByIdDescLimit(
            frontend = frontend,
            chatNativeId = chatNativeId,
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
            frontend = frontend,
            chatId = chatNativeId
        )

        val generatedMessages = generateMessage(messages = maskedMessages, chatConfig = chatConfig)
        val filteredMessages = filterFirstByAuthor(generatedMessages, chatConfig.username)

        for (message in filteredMessages) {
            gatewayMessageSenderController.sendTypingAction(
                frontend = frontend,
                chatId = chatNativeId
            )

            gatewayMessageSenderController.sendText(
                request = SendTextMessageRequest(
                    frontend = frontend,
                    text = message.content,
                    chatId = chatNativeId,
                    replyToMessageId = message.replyToMessageId,
                )
            )
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
