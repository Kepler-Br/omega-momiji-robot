package com.momiji.bot.repository

import com.momiji.bot.repository.entity.enumerator.MediaType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

data class MessageWithUserEntity(
    val replyToMessageNativeId: String?,
    val text: String?,
    val mediaLink: String?,
    val mediaType: MediaType?,
    val nativeId: String,
    val userNativeId: String,
    var fullname: String,
)

@Repository
interface MessageWithUserRepository : CrudRepository<MessageWithUserEntity, Unit> {
    @Query(
        """
            select m.text, m.media_link, u.fullname, m.media_type, m.native_id, m.reply_to_message_native_id, u.nativeId as user_native_id
            from gateway.messages m
                     inner join gateway.chats c on c.id = m.chat_id
                     inner join gateway.users u on m.user_id = u.id
            where m.frontend = :frontend
              and c.native_id = :chatNativeId
            order by m.id desc
            limit :limit;
        """
    )
    fun getByFrontendAndChatNativeIdOrderByIdDescLimit(
        frontend: String,
        chatNativeId: String,
        limit: Int
    ): List<MessageWithUserEntity>
}
