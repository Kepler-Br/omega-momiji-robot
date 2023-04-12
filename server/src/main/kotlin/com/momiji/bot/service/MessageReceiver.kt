package com.momiji.bot.service

interface MessageReceiver {
    fun process(dispatchedMessage: DispatchedMessage)
}
