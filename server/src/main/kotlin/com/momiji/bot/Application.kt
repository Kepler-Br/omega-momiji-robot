package com.momiji.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.momiji.bot.config")
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
