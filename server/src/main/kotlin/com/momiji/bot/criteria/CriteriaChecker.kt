package com.momiji.bot.criteria

import com.momiji.bot.service.data.DispatchedMessageEvent

interface CriteriaChecker {

    fun check(message: DispatchedMessageEvent): Boolean
}
