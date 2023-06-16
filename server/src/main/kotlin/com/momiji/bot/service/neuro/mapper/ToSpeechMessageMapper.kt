package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.caption.speech.SpeechCaptionClientService
import com.momiji.api.neural.generation.text.model.Message
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client

@Service
class ToSpeechMessageMapper(
    s3Client: S3Client,
    private val speechCaptionClientService: SpeechCaptionClientService,
) : AbstractToMessageMapper(
    s3Client,
    setOf(MediaType.VOICE)
) {
    override fun processFile(file: ByteArray, source: MessageWithUserEntity): Message {
        return Message(
            messageType = MessageType.VOICE,
            content = speechCaptionClientService.requestCaptionBlocking(data = file),
            author = source.fullname,
            messageId = source.nativeId,
            replyToMessageId = source.replyToMessageNativeId,
        )
    }
}
