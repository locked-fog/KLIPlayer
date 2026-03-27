package com.lockedfog.kliplayer.exception

/**
 * 执行异常：脚本执行过程中的错误
 * @param position 发生错误的时间位置（可选）
 */
class ExecutionException(
    message: String,
    val position: String? = null
) : KlipException(formatMessage(message, position)) {

    private companion object {
        fun formatMessage(message: String, position: String?): String {
            return position?.let { "[$it] $message" } ?: message
        }
    }
}
