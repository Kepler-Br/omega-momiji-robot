package com.momiji.bot.controller

import com.momiji.bot.api.ReceiveMessageEventsApi
import com.momiji.bot.api.model.NewMessageRequest
import com.momiji.bot.service.NewMessageService
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DefaultReceiveMessageEventsApi(
    private val newMessageService: NewMessageService,
) : ReceiveMessageEventsApi {

    override fun newMessage(@RequestBody request: NewMessageRequest) {
        newMessageService.newMessage(request)
    }
}
