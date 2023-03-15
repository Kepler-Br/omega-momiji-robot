package com.momiji.bot.api

import com.momiji.bot.api.model.NewMessageRequest
import org.springframework.web.bind.annotation.PostMapping


interface ReceiveMessageEventsApi {

    @PostMapping("messages")
    fun newMessage(request: NewMessageRequest)
}
