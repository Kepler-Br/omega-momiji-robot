package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessageEvent

class NotAnUpdatedMessage : CriteriaChecker {
    override fun check(message: DispatchedMessageEvent): Boolean {
        return !message.isUpdated
    }
}
