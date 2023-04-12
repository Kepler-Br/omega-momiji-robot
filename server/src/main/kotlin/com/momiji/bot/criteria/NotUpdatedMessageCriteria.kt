package com.momiji.bot.criteria

import com.momiji.bot.service.DispatchedMessage

class NotUpdatedMessageCriteria : CriteriaChecker {
    override fun check(message: DispatchedMessage): Boolean {
        return !message.isUpdated
    }
}
