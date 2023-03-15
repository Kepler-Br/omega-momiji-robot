package com.momiji.bot.service

import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.bot.repository.ChatRepository
import com.momiji.bot.repository.MessageRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

class NeuroMessageProcessorService(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
    @Value("\${ro-bot.context-size:10}")
    private val contextSize: Int,
) : MessageProcessorService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun process(request: NewMessageRequest) {
        if (request.isUpdated) {
            return
        }

        val (frontend, chatId, messageId, _) = request

        val messages = messageRepository.getByFrontendAndNativeIdAndChatNativeIdOrderByIdDescLimit(
            frontend = frontend,
            nativeId = messageId,
            chatNativeId = chatId,
            limit = contextSize,
        )
    }
}
