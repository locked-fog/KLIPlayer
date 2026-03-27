package com.lockedfog.kliplayer.exception

/**
 * 渲染异常：终端渲染过程中的错误
 */
class RenderException(
    message: String,
    cause: Throwable? = null
) : KlipException(message, cause)
