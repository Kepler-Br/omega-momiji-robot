package com.momiji.bot.service

import com.momiji.bot.service.data.DispatchedMessageEvent

interface MessageReceiver {
    fun process(dispatchedMessageEvent: DispatchedMessageEvent)
}
