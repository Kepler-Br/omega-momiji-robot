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
class DispatcherConfig(
    val neuroMessageProcessorService: NeuroMessageReceiver,
    val neuroMessageCommandsReceiver: NeuroMessageCommandsReceiver,
    val messageDispatcher: MessageDispatcher,
) {

    @PostConstruct
    fun post() {
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
