package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.generation.text.model.Message
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType

interface ToMessageMapper {

    fun map(source: MessageWithUserEntity): Message

    fun getSupportedMediaTypes(): Set<MediaType>
}
