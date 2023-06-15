package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.generation.text.model.Message
import com.momiji.bot.repository.entity.MessageWithUserEntity

interface ToMessageMapper {

    fun map(source: MessageWithUserEntity): Message
}
