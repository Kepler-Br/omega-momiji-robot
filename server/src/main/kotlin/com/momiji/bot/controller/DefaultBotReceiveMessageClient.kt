package com.momiji.bot.controller

import com.momiji.api.bot.BotReceiveMessageClient
import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.common.model.ResponseStatus
import com.momiji.api.common.model.SimpleResponse
import com.momiji.bot.service.MessageDispatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultBotReceiveMessageClient(
//    private val messageProcessorService: MessageProcessorService,
    private val messageDispatcher: MessageDispatcher,
) : BotReceiveMessageClient {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun newMessage(@RequestBody request: NewMessageRequest): SimpleResponse {
        logger.debug("New message: {}", request)

        messageDispatcher.process(request)

        return SimpleResponse(
            status = ResponseStatus.OK,
        )
    }
}
