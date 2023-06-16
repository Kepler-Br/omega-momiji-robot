package com.momiji.bot.service.neuro

import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderClient
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.api.neural.generation.text.TextGenerationClient
import com.momiji.api.neural.generation.text.model.*
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.data.DispatchedMessageEvent
import com.momiji.bot.service.neuro.mapper.ToMessageMapperService
import java.util.*
import kotlin.random.Random
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.momiji.api.neural.generation.text.model.Message as TextGenerationMessage


@Service
class NeuroMessageReceiver(
    private val messageWithUserRepository: MessageWithUserRepository,
    private val gatewayMessageSenderClient: GatewayMessageSenderClient,
    private val chatConfigService: ChatConfigService,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationClient: TextGenerationClient,
    private val toMessageMapperService: ToMessageMapperService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private fun maskFullname(userNativeId: String, original: String, target: String): String {
        return if (userNativeId == "SELF") {
            target
        } else if (original == target) {
            original.hashCode().toString(16).trimStart('-')
        } else {
            original
        }
    }

    fun process(dispatchedMessageEvent: DispatchedMessageEvent) {
        val chatNativeId = dispatchedMessageEvent.chat.nativeId
        val frontend = dispatchedMessageEvent.frontend

        val chatConfig =
            chatConfigService.getChatConfig(
                frontend = frontend,
                chatNativeId = chatNativeId
            )

        if (
            dispatchedMessageEvent.message.replyTo?.user?.nativeId != "SELF"
            && Random.nextFloat() > chatConfig.replyChance
        ) {
            logger.trace("Not hitting a replyChance of ${chatConfig.replyChance}. Skipping")

            return
        }

        val messages = messageWithUserRepository.getByFrontendAndChatNativeIdOrderByIdDescLimit(
            frontend = frontend,
            chatNativeId = chatNativeId,
            limit = contextSize,
        ).reversed()

        val processedMessages = messages.map {
            it.apply {
                this.fullname = maskFullname(it.userNativeId, it.fullname, chatConfig.username)
            }

            toMessageMapperService.map(it)
        }

        gatewayMessageSenderClient.sendTypingAction(
            frontend = frontend,
            chatId = chatNativeId
        )

        val generatedMessages = generateMessage(processedMessages, chatConfig)
        val filteredMessages = filterFirstByAuthor(generatedMessages, chatConfig.username)

        for (message in filteredMessages) {
            // TODO: Make possible to send images and voice
            if (message.messageType != MessageType.TEXT || message.content == null) {
                logger.warn("Skipping a message because it is not a TEXT(actual value is${message.messageType}) or content is null")
                continue
            }

            gatewayMessageSenderClient.sendTypingAction(
                frontend = frontend,
                chatId = chatNativeId
            )

            gatewayMessageSenderClient.sendText(
                request = SendTextMessageRequest(
                    frontend = frontend,
                    text = message.content!!,
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

        val response = textGenerationClient.requestGenerationFromHistory(
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
            textGenerationClient.getGeneratedFromHistory(taskId = promptId, async = false)

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
