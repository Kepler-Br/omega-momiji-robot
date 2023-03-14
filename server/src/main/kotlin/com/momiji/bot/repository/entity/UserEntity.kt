package com.momiji.bot.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table


@Table("users")
data class UserEntity(
    @Id
    var id: Long? = null,
    var username: String,
    var fullname: String,
    var frontend: String,
    var nativeId: String,
)
