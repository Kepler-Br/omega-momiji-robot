package com.momiji.bot.service

import com.momiji.api.common.model.BaseResponse
import com.momiji.api.common.model.ChatAdminsResponse
import com.momiji.api.common.model.SendMessageResponse
import com.momiji.api.gateway.outbound.GatewayMessageSenderClient
import com.momiji.api.gateway.outbound.model.FrontendNamesResponse
import com.momiji.api.gateway.outbound.model.SendTextMessageRequest
import org.springframework.stereotype.Service

@Service
class GatewayService(
    private val gatewayMessageSenderClient: GatewayMessageSenderClient,
) {

    fun sendText(
        frontend: String,
        text: String,
        chatId: String,
        replyToMessageId: String? = null,
    ): SendMessageResponse {
        return gatewayMessageSenderClient.sendText(
            SendTextMessageRequest(
                frontend = frontend,
                text = text,
                chatId = chatId,
                replyToMessageId = replyToMessageId
            )
        )
    }

    fun sendTypingAction(
        frontend: String,
        chatId: String
    ): BaseResponse {
        return gatewayMessageSenderClient.sendTypingAction(frontend = frontend, chatId = chatId)
    }

    fun getFrontendNames(): FrontendNamesResponse {
        return gatewayMessageSenderClient.getFrontendNames()
    }

    fun getChatAdmins(
        chatId: String,
        frontend: String
    ): ChatAdminsResponse {
        return gatewayMessageSenderClient.getChatAdmins(chatId = chatId, frontend = frontend)
    }
}
