package com.momiji.bot.service.neuro.mapper.exception

class ToMessageMapperException(message: String?, cause: Throwable?) :
    RuntimeException(message, cause) {

    constructor(message: String) : this(message, null)

    constructor(cause: Throwable) : this(null, cause)
}
