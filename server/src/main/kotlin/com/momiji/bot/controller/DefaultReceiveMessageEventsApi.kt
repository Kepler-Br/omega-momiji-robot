package com.momiji.bot.controller

import com.momiji.api.bot.BotReceiveMessageController
import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.bot.service.MessageProcessorService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultReceiveMessageEventsApi(
    private val messageProcessorService: MessageProcessorService,
) : BotReceiveMessageController {

    override fun newMessage(@RequestBody request: NewMessageRequest) {
        messageProcessorService.process(request)
    }
}
