package com.lockedfog.kliplayer.exception

/**
 * KLIPlayer 基础异常类
 */
open class KlipException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
