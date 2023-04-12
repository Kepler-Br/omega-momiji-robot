package com.momiji.bot.criteria

import com.momiji.bot.service.DispatchedMessage

class NotACommandCriteria : CriteriaChecker {
    override fun check(message: DispatchedMessage): Boolean {
        return message.command == null
    }
}
