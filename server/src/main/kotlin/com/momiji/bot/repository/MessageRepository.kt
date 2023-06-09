package com.momiji.bot.repository

import com.momiji.bot.repository.entity.MessageEntity
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface MessageRepository : CrudRepository<MessageEntity, Long> {
    fun getByFrontendAndNativeIdAndChatId(
        frontend: String,
        nativeId: String,
        chatId: Long
    ): MessageEntity

    @Query(
        """
            select m.*
            from gateway.messages m
                     inner join gateway.chats c on c.id = m.chat_id
            where m.frontend = :frontend
              and m.native_id = :nativeId
              and c.native_id = :chatNativeId;
        """
    )
    fun getByFrontendAndNativeIdAndChatNativeId(
        frontend: String,
        nativeId: String,
        chatNativeId: String,
    ): MessageEntity

    @Query(
        """
            select m.*
            from gateway.messages m
                     inner join gateway.chats c on c.id = m.chat_id
            where m.frontend = :frontend
              and m.native_id = :nativeId
              and c.native_id = :chatNativeId;
        """
    )
    fun findByFrontendAndNativeIdAndChatNativeId(
        frontend: String,
        nativeId: String,
        chatNativeId: String,
    ): MessageEntity?

    @Query(
        """
            select m.*
            from gateway.messages m
                     inner join gateway.chats c on c.id = m.chat_id
            where m.frontend = :frontend
              and m.native_id = :nativeId
              and c.native_id = :chatNativeId
            order by m.id desc
            limit :limit;
        """
    )
    fun getByFrontendAndNativeIdAndChatNativeIdOrderByIdDescLimit(
        frontend: String,
        nativeId: String,
        chatNativeId: String,
        limit: Int
    ): List<MessageEntity>
}
