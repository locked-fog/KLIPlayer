package com.lockedfog.kliplayer.exception

/**
 * 解析异常：词法或语法分析错误
 * @param line 错误发生的行号
 * @param column 错误发生的列号
 */
class ParseException(
    message: String,
    val line: Int = 0,
    val column: Int = 0
) : KlipException(formatMessage(message, line, column)) {

    private companion object {
        fun formatMessage(message: String, line: Int, column: Int): String {
            return if (line > 0) "[$line:$column] $message" else message
        }
    }
}
