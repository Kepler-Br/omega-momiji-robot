package com.momiji.bot.repository

import com.momiji.bot.repository.entity.UserEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface UserRepository : CrudRepository<UserEntity, Long> {
    fun getByFrontendAndNativeId(frontend: String, nativeId: String): UserEntity
}
