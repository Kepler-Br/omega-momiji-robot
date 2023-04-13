package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessage

interface CriteriaChecker {

    fun check(message: DispatchedMessage): Boolean
}
