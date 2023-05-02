package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessageEvent

class NotACommand : CriteriaChecker {
    override fun check(message: DispatchedMessageEvent): Boolean {
        return message.command == null
    }
}
