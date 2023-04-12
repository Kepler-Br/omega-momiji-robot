package com.momiji.bot.criteria

import com.momiji.bot.service.DispatchedMessage

interface CriteriaChecker {

    fun check(message: DispatchedMessage): Boolean
}
