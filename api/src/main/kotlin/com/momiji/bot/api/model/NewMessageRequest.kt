package com.momiji.bot.api.model

import com.fasterxml.jackson.annotation.JsonProperty

data class NewMessageRequest(
    @JsonProperty("frontend")
    val frontend: String,
    @JsonProperty("chat_id")
    val chatId: String,
    @JsonProperty("message_id")
    val messageId: String,
    /**
     * True if message was updated in DB. Not a new message
     */
    @JsonProperty("is_updated")
    val isUpdated: Boolean,
)
