package com.momiji.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "s3")
@ConstructorBinding
data class S3ConfigurationProperties(
    val host: String,
    val port: String,
    val scheme: String,
    val path: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
)
