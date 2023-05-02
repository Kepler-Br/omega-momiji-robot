package com.momiji.bot.service.neuro

import com.momiji.bot.config.SuperUserConfigurationProperties
import com.momiji.bot.repository.entity.ChatEntity
import com.momiji.bot.repository.entity.ChatGenerationConfigEntity
import com.momiji.bot.service.GatewayService
import com.momiji.bot.service.data.DispatchedMessage
import com.momiji.bot.service.data.DispatchedMessageEvent
import com.momiji.bot.service.data.MessageCommand
import org.springframework.stereotype.Service


@Service
class NeuroMessageCommandsReceiver(
    private val gatewayService: GatewayService,
    private val chatConfigService: ChatConfigService,
    private var superUserConfigurationProperties: SuperUserConfigurationProperties,
) {
    private val argumentIsNotAnInt: String = "Аргумент не целое число"

    private val argumentIsNotAFloat: String = "Аргумент не дробное число"


    fun process(dispatchedMessageEvent: DispatchedMessageEvent) {
        val command = dispatchedMessageEvent.command!!

        val frontend = dispatchedMessageEvent.frontend
        val chat = dispatchedMessageEvent.chat
        val message = dispatchedMessageEvent.message

        // Commands that does not require admin permissions
        when (command.command) {
            "whois" -> {
                whois(chat, message, frontend)
                return
            }

            "params" -> {
                params(chat, message, frontend)
                return
            }
        }

        if (command.arguments.isEmpty()) {
            return
        }

        // Commands that require admin permissions
        val isPermitted = userIsAdminOrSuperuser(
            userNativeId = dispatchedMessageEvent.user.nativeId,
            frontend = frontend,
            chatNativeId = chat.nativeId
        )

        if (!isPermitted) {
            gatewayService.sendText(
                frontend = frontend,
                text = "Ты не существуешь",
                chatId = chat.nativeId,
                replyToMessageId = message.nativeId
            )
        }

        val config =
            chatConfigService.getChatConfig(frontend = frontend, chatNativeId = chat.nativeId)

        when (command.command) {
            "chme" -> chme(command, chat, message, frontend, config)
            "max_new_tokens" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAnInt) {
                    it.maxNewTokens = command.arguments[0].toInt()
                }

            "num_beams" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAnInt) {
                    it.numBeams = command.arguments[0].toInt()
                }

            "no_repeat_ngram_size" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAnInt) {
                    it.noRepeatNgramSize = command.arguments[0].toInt()
                }

            "temperature" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAFloat) {
                    it.temperature = command.arguments[0].toFloat()
                }

            "top_p" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAFloat) {
                    it.topP = command.arguments[0].toFloat()
                }

            "top_k" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAnInt) {
                    it.topK = command.arguments[0].toInt()
                }

            "repetition_penalty" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAFloat) {
                    it.repetitionPenalty = command.arguments[0].toFloat()
                }

            "reply_chance" ->
                updateConfig(chat, message, frontend, config, argumentIsNotAFloat) {
                    it.replyChance = command.arguments[0].toFloat()
                }
        }
    }

    private fun getParamsHelp(config: ChatGenerationConfigEntity): String {
        return """
                /chme ${config.username}
                /max_new_tokens ${config.maxNewTokens}
                /num_beams ${config.numBeams}
                /no_repeat_ngram_size ${config.noRepeatNgramSize}
                /temperature ${config.temperature}
                /top_p ${config.topP}
                /top_k ${config.topK}
                /repetition_penalty ${config.repetitionPenalty}
                /reply_chance ${config.replyChance}
                """.trimIndent()
    }

    private fun userIsAdminOrSuperuser(
        userNativeId: String,
        frontend: String,
        chatNativeId: String
    ): Boolean {
        if (frontend in superUserConfigurationProperties.frontend
            && userNativeId in superUserConfigurationProperties.frontend[frontend]!!
        ) {
            return true
        }

        val admins = gatewayService.getChatAdmins(
            chatId = chatNativeId,
            frontend = frontend
        )

        return userNativeId in admins.adminIds
    }

    private fun whois(
        chat: ChatEntity,
        message: DispatchedMessage,
        frontend: String
    ) {
        val config =
            chatConfigService.getChatConfig(frontend = frontend, chatNativeId = chat.nativeId)

        gatewayService.sendText(
            frontend = frontend,
            text = "Я ${config.username}",
            chatId = chat.nativeId,
            replyToMessageId = message.nativeId
        )
    }

    private fun params(
        chat: ChatEntity,
        message: DispatchedMessage,
        frontend: String
    ) {
        val config =
            chatConfigService.getChatConfig(frontend = frontend, chatNativeId = chat.nativeId)

        chatConfigService.save(config)

        gatewayService.sendText(
            frontend = frontend,
            text = getParamsHelp(config),
            chatId = chat.nativeId,
            replyToMessageId = message.nativeId
        )
    }

    private fun updateConfig(
        chat: ChatEntity,
        message: DispatchedMessage,
        frontend: String,
        config: ChatGenerationConfigEntity,
        onExceptionMessage: String,
        setter: (ChatGenerationConfigEntity) -> Unit,
    ) {
        try {
            setter(config)
        } catch (_: RuntimeException) {
            gatewayService.sendText(
                frontend = frontend,
                text = onExceptionMessage,
                chatId = chat.nativeId
            )

            return
        }

        chatConfigService.save(config)

        gatewayService.sendText(
            frontend = frontend,
            text = getParamsHelp(config),
            chatId = chat.nativeId,
            replyToMessageId = message.nativeId
        )
    }

    private fun chme(
        command: MessageCommand,
        chat: ChatEntity,
        message: DispatchedMessage,
        frontend: String,
        config: ChatGenerationConfigEntity,
    ) {
        config.username = command.arguments.joinToString(" ")

        chatConfigService.save(config)

        gatewayService.sendText(
            frontend = frontend,
            text = "Теперь я ${config.username}",
            chatId = chat.nativeId,
            replyToMessageId = message.nativeId
        )
    }
}
