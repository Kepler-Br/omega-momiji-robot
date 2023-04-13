package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessage

class IsACommand : CriteriaChecker {
    override fun check(message: DispatchedMessage): Boolean {
        return message.command == null
    }
}
