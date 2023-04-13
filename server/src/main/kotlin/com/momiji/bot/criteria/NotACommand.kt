package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessage

class NotACommand : CriteriaChecker {
    override fun check(message: DispatchedMessage): Boolean {
        return message.command == null
    }
}
