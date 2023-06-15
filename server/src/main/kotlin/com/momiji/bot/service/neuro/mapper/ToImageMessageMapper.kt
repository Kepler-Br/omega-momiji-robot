package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.caption.image.ImageCaptionClientService
import com.momiji.api.neural.generation.text.model.Message
import com.momiji.api.neural.generation.text.model.MessageType
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Service
class ToImageMessageMapper(
    private val s3Client: S3Client,
    private val imageCaptionClientService: ImageCaptionClientService,
) : ToMessageMapper {
    override fun map(source: MessageWithUserEntity): Message {
        if (source.mediaType == null
            || (source.mediaType != MediaType.STICKER
                    && source.mediaType != MediaType.PHOTO)
        ) {
            throw ToMessageMapperException("Null or not an expected media type: ${source.mediaType}")
        }

        if (source.s3bucket == null || source.s3key == null) {
            throw ToMessageMapperException("S3 bucket and/or key is null")
        }

        val fileBytes = try {
            s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket(source.s3bucket)
                    .key(source.s3key)
                    .build()
            ).readAllBytes()
        } catch (ex: S3Exception) {
            throw ToMessageMapperException(
                "Failed to get a file from S3. " +
                        "Bucket: \"${source.s3bucket}\". Key: \"${source.s3key}\"",
                ex
            )
        }

        val caption = imageCaptionClientService.requestCaptionBlocking(
            data = fileBytes,
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
