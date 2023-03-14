package com.momiji.bot.service

import com.momiji.bot.api.model.NewMessageRequest
import com.momiji.bot.repository.MessageRepository
import com.momiji.gateway.outbound.api.GatewayMessageSenderController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NewMessageService(
    private val messageRepository: MessageRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun newMessage(request: NewMessageRequest) {
        logger.info(
            "A new message was received " +
                    "from frontend ${request.frontend}, " +
                    "with chat id ${request.chatId}, " +
                    "with message id ${request.messageId}"
        )
    }
}
