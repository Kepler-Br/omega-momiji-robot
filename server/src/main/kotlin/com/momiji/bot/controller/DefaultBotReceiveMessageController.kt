package com.momiji.bot.controller

import com.momiji.api.bot.BotReceiveMessageController
import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.api.common.model.BasicResponse
import com.momiji.api.common.model.ResponseStatus
import com.momiji.bot.service.MessageProcessorService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultBotReceiveMessageController(
    private val messageProcessorService: MessageProcessorService,
) : BotReceiveMessageController {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun newMessage(@RequestBody request: NewMessageRequest): BasicResponse {
        logger.debug("New message: $request")

        messageProcessorService.process(request)

        return BasicResponse(
            status = ResponseStatus.OK,
        )
    }
}
