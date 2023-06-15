package com.momiji.bot.repository.entity

import com.momiji.bot.repository.entity.enumerator.MediaType

data class MessageWithUserEntity(
    val replyToMessageNativeId: String?,
    val text: String?,
    val s3bucket: String?,
    val s3key: String?,
    val mediaType: MediaType?,
    val nativeId: String,
    val userNativeId: String,
    var fullname: String,
)
