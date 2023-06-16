package com.momiji.bot.service.neuro.mapper

import com.momiji.api.neural.generation.text.model.Message
import com.momiji.bot.repository.entity.MessageWithUserEntity
import com.momiji.bot.repository.entity.enumerator.MediaType
import com.momiji.bot.service.neuro.mapper.exception.ToMessageMapperException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

abstract class AbstractToMessageMapper(
    private val s3Client: S3Client,
    private val supportedMediaTypes: Set<MediaType>,
) : ToMessageMapper {
    override fun map(source: MessageWithUserEntity): Message {
        if (source.mediaType == null || source.mediaType !in supportedMediaTypes) {
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

        return processFile(fileBytes, source)
    }

    override fun getSupportedMediaTypes(): Set<MediaType> {
        return supportedMediaTypes
    }

    protected abstract fun processFile(file: ByteArray, source: MessageWithUserEntity): Message
}
