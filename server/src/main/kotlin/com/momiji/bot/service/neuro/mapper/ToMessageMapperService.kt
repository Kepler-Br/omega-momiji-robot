package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.generation.text.model.Message
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType
import java.util.EnumMap
import org.springframework.stereotype.Service

@Service
class ToMessageMapperService(
    mappers: List<ToMessageMapper>,
) {

    private val mappers: MutableMap<MediaType, ToMessageMapper> = EnumMap(MediaType::class.java)

    init {
        for (mapper in mappers) {
            for (mediaType in mapper.getSupportedMediaTypes()) {
                if (mediaType in this.mappers) {
                    throw RuntimeException("Overlapping mappers for MediaType. Possibly a development bug")
                }

                this.mappers[mediaType] = mapper
            }
        }
    }

    fun map(source: MessageWithUserEntity): Message {
        // Simple text message
        if (source.mediaType == null) {
            return Message(
                messageType = MessageType.TEXT,
                content = source.text!!,
                author = source.fullname,
                messageId = source.nativeId,
                replyToMessageId = source.replyToMessageNativeId,
            )
        }

        val mapper = mappers[source.mediaType]

        // Have a mapper for media type
        if (mapper != null) {
            return mapper.map(source)
        }

        // Unmappable mediaType
        val mediaType = when(source.mediaType) {
            MediaType.STICKER -> MessageType.IMAGE
            MediaType.AUDIO -> MessageType.AUDIO
            MediaType.VOICE -> MessageType.VOICE
            MediaType.PHOTO -> MessageType.IMAGE
            MediaType.VIDEO -> MessageType.VIDEO
            MediaType.GIF -> MessageType.IMAGE
            MediaType.VIDEO_NOTE -> MessageType.VIDEO
        }

        return Message(
            messageType = mediaType,
            content = null,
            author = source.fullname,
            messageId = source.nativeId,
            replyToMessageId = source.replyToMessageNativeId,
        )
    }
}
