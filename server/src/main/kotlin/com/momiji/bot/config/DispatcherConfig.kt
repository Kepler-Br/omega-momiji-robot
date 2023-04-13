package com.momiji.bot.config

import com.momiji.bot.criteria.IsACommand
import com.momiji.bot.criteria.NotACommand
import com.momiji.bot.criteria.NotAnUpdatedMessage
import com.momiji.bot.service.MessageDispatcher
import com.momiji.bot.service.neuro.NeuroMessageCommandsReceiver
import com.momiji.bot.service.neuro.NeuroMessageReceiver
import javax.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class DispatcherConfig {

    @PostConstruct
    fun asd(
        neuroMessageProcessorService: NeuroMessageReceiver,
        neuroMessageCommandsReceiver: NeuroMessageCommandsReceiver,
        messageDispatcher: MessageDispatcher,
    ) {

        messageDispatcher.register(
            listOf(NotACommand(), NotAnUpdatedMessage()),
            neuroMessageProcessorService::process
        )

        messageDispatcher.register(
            listOf(IsACommand(), NotAnUpdatedMessage()),
            neuroMessageCommandsReceiver::process
        )
    }
}
