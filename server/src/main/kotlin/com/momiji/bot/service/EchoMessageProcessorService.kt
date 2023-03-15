package com.momiji.bot.service

import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.bot.repository.ChatRepository
import com.momiji.bot.repository.MessageRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EchoMessageProcessorService(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
) : MessageProcessorService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private fun echo(messageId: String, chatId: String, frontend: String) {
        val message = messageRepository.getByFrontendAndNativeIdAndChatNativeId(
            frontend = frontend, nativeId = messageId, chatNativeId = chatId
        )

        gatewayMessageSenderController.sendText(
            SendTextMessageRequest(
                frontend = frontend,
                text = message.text ?: message.mediaType?.toString() ?: "Echo",
                chatId = chatId,
            )
        )
    }

    override fun process(request: NewMessageRequest) {
        logger.info(
            "A new message was received " +
                    "from frontend '${request.frontend}', " +
                    "with chat id '${request.chatId}', " +
                    "with message id '${request.messageId}'"
        )

        if (!request.isUpdated) {
            echo(
                messageId = request.messageId,
                chatId = request.chatId,
                frontend = request.frontend
            )
        }
    }
}
