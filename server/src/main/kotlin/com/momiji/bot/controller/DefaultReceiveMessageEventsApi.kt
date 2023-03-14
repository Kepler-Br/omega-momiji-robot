package com.momiji.bot.controller

import com.momiji.bot.api.ReceiveMessageEventsApi
import com.momiji.bot.api.model.NewMessageRequest
import com.momiji.bot.service.NewMessageService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
class DefaultReceiveMessageEventsApi(
    private val newMessageService: NewMessageService,
) : ReceiveMessageEventsApi {

    override fun newMessage(request: NewMessageRequest) {
        newMessageService.newMessage(request)
    }
}
