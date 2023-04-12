package com.momiji.bot.config

import com.momiji.bot.criteria.NotACommandCriteria
import com.momiji.bot.service.MessageDispatcher
import com.momiji.bot.service.neuro.NeuroMessageReceiver
import javax.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class DispatcherConfig {

    @PostConstruct
    fun asd(
        neuroMessageProcessorService: NeuroMessageReceiver,
        messageDispatcher: MessageDispatcher,
    ) {

        messageDispatcher.register(
            listOf(NotACommandCriteria()),
            neuroMessageProcessorService::process
        )
    }
}
