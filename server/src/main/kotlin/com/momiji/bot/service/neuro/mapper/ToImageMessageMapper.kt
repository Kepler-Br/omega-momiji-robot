package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.caption.image.ImageCaptionClientService
import com.momiji.api.neural.generation.text.model.Message
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client

@Service
class ToImageMessageMapper(
    s3Client: S3Client,
    private val imageCaptionClientService: ImageCaptionClientService,
) : AbstractToMessageMapper(
    s3Client,
    setOf(MediaType.STICKER, MediaType.PHOTO)
) {
    override fun processFile(file: ByteArray, source: MessageWithUserEntity): Message {
        val caption = imageCaptionClientService.requestCaptionBlocking(
            data = file,
            condition = null,
        )

        return Message(
            messageType = if (source.mediaType == MediaType.PHOTO) {
                MessageType.IMAGE
            } else {
                MessageType.STICKER
            },
            content = caption,
            author = source.fullname,
            messageId = source.nativeId,
            replyToMessageId = source.replyToMessageNativeId,
        )
    }
}
