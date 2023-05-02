package com.momiji.bot.service

import com.momiji.api.bot.model.NewMessageRequest
import com.momiji.bot.criteria.CriteriaChecker
import com.momiji.bot.repository.ChatRepository
import com.momiji.bot.repository.MessageRepository
import com.momiji.bot.repository.UserRepository
import com.momiji.bot.service.data.DispatchedMessage
import com.momiji.bot.service.data.DispatchedMessageEvent
import com.momiji.bot.service.data.DispatchedRepliedMessage
import com.momiji.bot.service.data.MessageCommand
import java.util.concurrent.Executors
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


typealias TargetCallable = (DispatchedMessageEvent) -> Unit
typealias TargetPair = Pair<TargetCallable, List<CriteriaChecker>>

@Service
class MessageDispatcher(
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) {

    private val targets = ArrayList<TargetPair>()

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val threadExecutor = Executors.newFixedThreadPool(5)


    private fun extractCommand(text: String?): MessageCommand? {
        if (
            text == null
            || (!text.startsWith('/') && text.length > 1)
        ) {
            return null
        }

        val splitted = text.split(' ')

        val command = splitted[0].removePrefix("/")

        val arguments = if (splitted.size > 1) {
            splitted.subList(1, splitted.size)
        } else {
            emptyList()
        }

        if (command.length <= 1) {
            return null
        }

        return MessageCommand(
            command = command,
            arguments = arguments
        )
    }

    private fun getDispatchedMessage(request: NewMessageRequest): DispatchedMessageEvent {
        val message = messageRepository.getByFrontendAndNativeIdAndChatNativeId(
            frontend = request.frontend,
            nativeId = request.messageId,
            chatNativeId = request.chatId
        )
        val user = userRepository.findByIdOrNull(message.userId!!)!!
        val chat = chatRepository.findByIdOrNull(message.chatId!!)!!

        val replyToMessage = if (message.replyToMessageNativeId != null) {
            val replyTo = messageRepository.findByFrontendAndNativeIdAndChatNativeId(
                frontend = request.frontend,
                nativeId = message.replyToMessageNativeId!!,
                chatNativeId = request.chatId
            )

            if (replyTo != null) {
                val replyToUser = userRepository.findByIdOrNull(replyTo.userId!!)!!

                DispatchedRepliedMessage(
                    id = replyTo.id,
                    text = replyTo.text,
                    mediaLink = replyTo.mediaLink,
                    mediaType = replyTo.mediaType,
                    user = replyToUser,
                    replyToNativeId = replyTo.replyToMessageNativeId,
                    nativeId = replyTo.nativeId,
                )
            } else {
                null
            }
        } else {
            null
        }

        return DispatchedMessageEvent(
            user = user,
            chat = chat,
            message = DispatchedMessage(
                id = message.id,
                text = message.text,
                mediaLink = message.mediaLink,
                mediaType = message.mediaType,
                user = user,
                replyTo = replyToMessage,
                nativeId = message.nativeId,
            ),
            command = extractCommand(message.text),
            isUpdated = request.isUpdated,
            frontend = request.frontend,
        )
    }

    fun register(
        criteria: List<CriteriaChecker>,
        callable: TargetCallable
    ) {
        targets.add(Pair(callable, criteria))
    }

    fun process(request: NewMessageRequest) {
        // TODO: Read about propagating TraceID to task. See: LazyTraceThreadPoolTaskExecutor
        threadExecutor.submit {
            try {
                val message = getDispatchedMessage(request)

                for ((callable, criteriaList) in targets) {
                    val result = criteriaList
                        .fold(true) { acc, it -> acc && it.check(message) }

                    if (result) {
                        callable(message)
                        break
                    }
                }
            } catch (ex: RuntimeException) {
                logger.error(
                    "Exception has occurred during processing message in thread executor",
                    ex
                )
            }
        }
    }
}
