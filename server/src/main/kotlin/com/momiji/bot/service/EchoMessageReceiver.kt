package com.momiji.bot.service

import com.momiji.api.gateway.outbound.GatewayMessageSenderClient
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import com.momiji.bot.repository.MessageRepository
import com.momiji.bot.service.data.DispatchedMessageEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EchoMessageReceiver(
    private val messageRepository: MessageRepository,
    private val gatewayMessageSenderClient: GatewayMessageSenderClient,
) : MessageReceiver {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun process(dispatchedMessageEvent: DispatchedMessageEvent) {
        logger.info(
            "A new message was received " +
                    "from frontend '${dispatchedMessageEvent.frontend}', " +
                    "with chat id '${dispatchedMessageEvent.chat.nativeId}', " +
                    "with message id '${dispatchedMessageEvent.message.nativeId}'"
        )

        val message = messageRepository.getByFrontendAndNativeIdAndChatNativeId(
            frontend = dispatchedMessageEvent.frontend,
            nativeId = dispatchedMessageEvent.message.nativeId,
            chatNativeId = dispatchedMessageEvent.chat.nativeId
        )

        gatewayMessageSenderClient.sendText(
            SendTextMessageRequest(
                frontend = dispatchedMessageEvent.frontend,
                text = message.text ?: message.mediaType?.toString() ?: "Echo",
                chatId = dispatchedMessageEvent.chat.nativeId,
            )
        )
    }
}
