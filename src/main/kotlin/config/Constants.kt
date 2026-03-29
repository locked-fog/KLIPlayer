package com.lockedfog.kliplayer.config

/**
 * ANSI 转义码常量
 * 统一提供完整的、可直接输出的转义字符串
 */
object AnsiCodes {
    const val PREFIX = "\u001B["
    const val SUFFIX = "m"

    /** 重置所有属性 (颜色和样式) */
    const val RESET_ALL = "${PREFIX}0$SUFFIX"

    /** 恢复默认前景色 */
    const val RESET_FG = "${PREFIX}39$SUFFIX"

    /** 恢复默认背景色 */
    const val RESET_BG = "${PREFIX}49$SUFFIX"

    /** 隐藏终端光标 */
    const val HIDE_CURSOR = "${PREFIX}?25l"

    /** 显示终端光标 */
    const val SHOW_CURSOR = "${PREFIX}?25h"

    /** 清除整个屏幕 */
    const val CLEAR_SCREEN = "${PREFIX}2J"

    /** 清除当前行 */
    const val CLEAR_LINE = "${PREFIX}2K"

    /** * 前景色 RGB
     * @return 完整的 ANSI 转义字符串
     */
    fun foreground(r: Int, g: Int, b: Int) = "${PREFIX}38;2;$r;$g;${b}$SUFFIX"

    /** * 背景色 RGB
     * @return 完整的 ANSI 转义字符串
     */
    fun background(r: Int, g: Int, b: Int) = "${PREFIX}48;2;$r;$g;${b}$SUFFIX"

    /** * 光标移动到指定位置 (1-indexed)
     * @return 完整的 ANSI 转义字符串
     */
    fun cursorMove(row: Int, col: Int) = "${PREFIX}${row};${col}H"
}

/**
 * 文本样式类型
 * 包含了开启和关闭状态的 ANSI 码
 */
enum class StyleType(val onCode: String, val offCode: String) {
    BOLD("1", "22"),
    ITALIC("3", "23"),
    UNDERLINE("4", "24"),
    STRIKETHROUGH("9", "29");

    /** 获取开启该样式的完整 ANSI 字符串 */
    fun enableString() = "${AnsiCodes.PREFIX}${onCode}${AnsiCodes.SUFFIX}"

    /** 获取关闭该样式的完整 ANSI 字符串 */
    fun disableString() = "${AnsiCodes.PREFIX}${offCode}${AnsiCodes.SUFFIX}"

    companion object {
        fun fromName(name: String): StyleType? {
            return when (name.lowercase()) {
                "bold" -> BOLD
                "italic" -> ITALIC
                "underline" -> UNDERLINE
                "strikeline" -> STRIKETHROUGH
                else -> null
            }
        }
    }
}

/**
 * 脚本关键字
 */
object Keywords {
    const val META = "meta"
    const val TIMELINE = "timeline"
    const val NEW_CURSOR = "newcursor"
    const val CURSOR = "cursor"
    const val MV = "mv"
    const val HIDE = "hide"
    const val SHOW = "show"
    const val COLOR = "color"
    const val BACKGROUND = "background"
    const val STYLE = "style"
    const val LEVEL = "level"
    const val PROTECT = "protect"
    const val CLEAN = "clean"
    const val CLEANLINE = "cleanline"
    const val NEWLINE = "newline"
    const val DELCURSOR = "delcursor"
    const val SPACE = "space"
    const val BACKSPACE = "backspace"
    const val UTF8 = "utf-8"
    const val IMG = "img"
    const val BPM = "bpm"
    const val MACRO_END = "endmacro"
    const val LOOP = "loop"
    const val ENDLOOP = "endloop"
    const val MACRO_VAL = "@"
    const val MACRO = "#"
    const val COROUTINE = "$"
    val ALL_KEYS = setOf(
        META,TIMELINE,NEW_CURSOR,CURSOR,MV,HIDE,SHOW,COLOR,
        BACKGROUND,STYLE,LEVEL,PROTECT,CLEAN,CLEANLINE,NEWLINE,
        DELCURSOR,SPACE,BACKSPACE,UTF8,IMG,BPM,
        MACRO_END,LOOP,ENDLOOP,MACRO_VAL,MACRO,COROUTINE
    )
}