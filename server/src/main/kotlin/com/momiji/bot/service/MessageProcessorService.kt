package com.momiji.bot.service

import com.momiji.api.bot.model.NewMessageRequest

interface MessageProcessorService {
    fun process(request: NewMessageRequest)
}
