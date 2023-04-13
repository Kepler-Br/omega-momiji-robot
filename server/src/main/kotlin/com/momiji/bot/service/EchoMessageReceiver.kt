package com.momiji.bot.service

import com.momiji.api.gateway.outbound.GatewayMessageSenderController
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.bot.repository.ChatRepository
import com.momiji.bot.repository.MessageRepository
import com.momiji.bot.service.data.DispatchedMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EchoMessageReceiver(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val gatewayMessageSenderController: GatewayMessageSenderController,
) : MessageReceiver {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun process(dispatchedMessage: DispatchedMessage) {
        logger.info(
            "A new message was received " +
                    "from frontend '${dispatchedMessage.frontend}', " +
                    "with chat id '${dispatchedMessage.chat.nativeId}', " +
                    "with message id '${dispatchedMessage.message.nativeId}'"
        )

        val message = messageRepository.getByFrontendAndNativeIdAndChatNativeId(
            frontend = dispatchedMessage.frontend,
            nativeId = dispatchedMessage.message.nativeId,
            chatNativeId = dispatchedMessage.chat.nativeId
        )

        gatewayMessageSenderController.sendText(
            SendTextMessageRequest(
                frontend = dispatchedMessage.frontend,
                text = message.text ?: message.mediaType?.toString() ?: "Echo",
                chatId = dispatchedMessage.chat.nativeId,
            )
        )
    }
}
