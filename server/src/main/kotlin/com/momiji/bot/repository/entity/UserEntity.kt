package com.momiji.bot.repository.entity

import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table


@Table(
    name = "users",
    schema = "gateway",
)
data class UserEntity(
    @Id
    var id: Long? = null,
    var createdAt: LocalDateTime? = null,
    var username: String,
    var fullname: String,
    var frontend: String,
    var nativeId: String,
)
