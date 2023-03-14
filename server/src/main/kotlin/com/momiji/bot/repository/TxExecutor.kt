package com.momiji.bot.repository

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TxExecutor {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> new(l: () -> T): T {
        return l()
    }
}
