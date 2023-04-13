package com.momiji.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "ro-bot.superusers")
data class SuperUserConfigurationProperties(
    val frontend: Map<String, Set<String>>,
)
