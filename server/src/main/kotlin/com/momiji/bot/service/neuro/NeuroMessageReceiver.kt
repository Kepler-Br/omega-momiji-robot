package com.momiji.bot.service.neuro

import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.gateway.outbound.GatewayMessageSenderService
import com.momiji.api.neural.generation.image.ImageGenerationClientService
import com.momiji.api.neural.generation.text.TextGenerationClient
import com.momiji.api.neural.generation.text.model.GenerationParams
import com.momiji.api.neural.generation.text.model.HistoryRequest
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.MessageWithUserRepository
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.data.DispatchedMessageEvent
import com.momiji.bot.service.neuro.mapper.ToMessageMapperService
import feign.FeignException
import java.util.UUID
import kotlin.random.Random
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import com.momiji.api.neural.generation.text.model.Message as TextGenerationMessage


@Service
class NeuroMessageReceiver(
    private val messageWithUserRepository: MessageWithUserRepository,
    private val imageGenerationClientService: ImageGenerationClientService,
    private val gatewayMessageSenderService: GatewayMessageSenderService,
    private val chatConfigService: ChatConfigService,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
    private val textGenerationClient: TextGenerationClient,
    private val toMessageMapperService: ToMessageMapperService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private fun maskFullname(
        userNativeId: String,
        originalName: String,
        nameToMask: String
    ): String {
        return if (userNativeId == "SELF") {
            nameToMask
        } else if (originalName == nameToMask) {
            originalName.hashCode().toString(16).trimStart('-')
        } else {
            originalName
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

        val processedMessages =
            messageWithUserRepository.getByFrontendAndChatNativeIdOrderByIdDescLimit(
                frontend = frontend,
                chatNativeId = chatNativeId,
                limit = contextSize,
            ).reversed().map {
                toMessageMapperService.map(
                    it.apply {
                        this.fullname =
                            maskFullname(it.userNativeId, it.fullname, chatConfig.username)
                    }
                )
            }

        gatewayMessageSenderService.sendTypingAction(
            frontend = frontend,
            chatId = chatNativeId
        )

        val generatedMessages = generateMessage(processedMessages, chatConfig)
        val filteredMessages = filterFirstByAuthor(generatedMessages, chatConfig.username)

        for (message in filteredMessages) {
            // TODO: Make possible to send images and voice
            when (message.messageType) {
                MessageType.TEXT -> {
                    sentTextMessage(message = message, frontend = frontend, chatNativeId = chatNativeId)
                }
                MessageType.IMAGE -> {
                    sendImageMessage(
                        message = message,
                        frontend = frontend,
                        chatNativeId = chatNativeId
                    )
                }
                else -> {
                    sendUnprocessableMessage(
                        message = message,
                        frontend = frontend,
                        chatNativeId = chatNativeId
                    )
                }
            }
        }
    }

    private fun sendUnprocessableMessage(
        message: TextGenerationMessage,
        frontend: String,
        chatNativeId: String
    ) {
        gatewayMessageSenderService.sendTextWithTyping(
            frontend = frontend,
            text = "${message.messageType}: ${message.content!!}",
            chatId = chatNativeId,
            replyToMessageId = message.replyToMessageId,
        )
    }

    private fun sendImageMessage(
        message: TextGenerationMessage,
        frontend: String,
        chatNativeId: String
    ) {
        if (message.content == null) {
            gatewayMessageSenderService.sendTextWithTyping(
                frontend = frontend,
                text = "[IMAGE]",
                chatId = chatNativeId,
                replyToMessageId = message.replyToMessageId,
            )
            return
        }
        try {
            gatewayMessageSenderService.sendImage(
                frontend = frontend,
                data = imageGenerationClientService.requestGenerationBlocking(
                    prompt = message.content!!
                ),
                chatId = chatNativeId,
                replyToMessageId = message.replyToMessageId
            )
        } catch (ex: FeignException.FeignClientException) {
            logger.error(
                "Exception while sending image generation request. Sending text message",
                ex
            )
            sendUnprocessableMessage(
                message = message, frontend = frontend, chatNativeId = chatNativeId
            )
        }
    }

    private fun sentTextMessage(
        message: TextGenerationMessage,
        frontend: String,
        chatNativeId: String
    ) {
        if (message.content == null) {
            logger.warn("No content produced for text message")
        } else {
            gatewayMessageSenderService.sendTextWithTyping(
                frontend = frontend,
                text = message.content!!,
                chatId = chatNativeId,
                replyToMessageId = message.replyToMessageId,
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
