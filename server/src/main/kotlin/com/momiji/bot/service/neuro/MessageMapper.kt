package com.momiji.bot.service.neuro

import com.momiji.api.neural.text.model.MessageType
import com.momiji.bot.repository.MessageWithUserEntity
import com.momiji.api.neural.text.model.Message as TextGenerationMessage

class MessageMapper {
    private val idMap: MutableMap<Int, String> = HashMap()
    private val reverseIdMap: MutableMap<String, Int> = HashMap()

    private fun maskName(fullname: String): String {
        return fullname.hashCode().toString(16)
    }

    fun mapAndSaveIds(
        value: List<MessageWithUserEntity>,
        nameToMask: String? = null
    ): List<TextGenerationMessage> {
        idMap.clear()
        reverseIdMap.clear()

        return value.mapIndexed { index, it ->
            idMap[index] = it.nativeId
            reverseIdMap[it.nativeId] = index

            val author = if (nameToMask != null && it.fullname == nameToMask) {
                maskName(it.fullname)
            } else {
                it.fullname
            }

            TextGenerationMessage(
                messageType = MessageType.TEXT,
                // TODO: Ideally, should never be null.
                content = it.text ?: "Unknown",
                author = author,
                messageId = reverseIdMap[it.nativeId]!!,
                replyToMessageId = reverseIdMap[it.replyToMessageNativeId],
            )
        }
    }

    fun mapId(id: Int?): String? {
        return idMap[id]
    }
}
