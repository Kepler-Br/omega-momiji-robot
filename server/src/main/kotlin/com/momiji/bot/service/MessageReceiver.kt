package com.momiji.bot.service

import com.momiji.bot.service.data.DispatchedMessage

interface MessageReceiver {
    fun process(dispatchedMessage: DispatchedMessage)
}
