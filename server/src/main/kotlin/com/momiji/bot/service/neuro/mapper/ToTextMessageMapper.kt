package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.generation.text.model.Message
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.entity.MessageWithUserEntity
import org.springframework.stereotype.Service

@Service
class ToTextMessageMapper : ToMessageMapper {
    override fun map(source: MessageWithUserEntity): Message {
        return Message(
            messageType = MessageType.TEXT,
            content = source.text!!,
            author = source.fullname,
            messageId = source.nativeId,
            replyToMessageId = source.replyToMessageNativeId,
        )
    }
}
