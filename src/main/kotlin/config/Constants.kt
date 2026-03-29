package com.lockedfog.kliplayer.config

import com.lockedfog.kliplayer.config.Keywords.HIDE
import com.lockedfog.kliplayer.config.Keywords.SHOW


/**
 * ANSI 转义码常量
 */
object AnsiCodes {
    const val PREFIX = "\u001B["
    const val SUFFIX = "m"

    /** 重置所有属性 */
    const val RESET = "0"

    /** 前景色 RGB */
    fun foreground(r: Int, g: Int, b: Int) = "38;2;$r;$g;$b"

    /** 背景色 RGB */
    fun background(r: Int, g: Int, b: Int) = "48;2;$r;$g;$b"

    /** 光标移动到指定位置 (1-indexed) */
    fun cursorMove(row: Int, col: Int) = "${PREFIX}${row};${col}H"

    /** 清除屏幕 */
    const val CLEAR_SCREEN = "2J"

    /** 清除当前行 */
    const val CLEAR_LINE = "2K"
}

/**
 * 文本样式类型
 */
enum class StyleType(val code: String) {
    BOLD("1"),
    ITALIC("3"),
    UNDERLINE("4"),
    STRIKETHROUGH("9");

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
    const val VAL = "val"
    const val VAR = "var"
    const val MACRO_START = "macro"
    const val MACRO_END = "endmacro"
    const val LOOP = "loop"
    const val ENDLOOP = "endloop"
    const val MACRO_VAL = "@"
    const val MACRO_VAR = "@@"
    const val MACRO = "#"
    const val COROUTINE = "$"
    const val ASSIGN = "="
    val ALL_KEYS = setOf(
        META,TIMELINE,NEW_CURSOR,CURSOR,MV,HIDE,SHOW,COLOR,
        BACKGROUND,STYLE,LEVEL,PROTECT,CLEAN,CLEANLINE,NEWLINE,
        DELCURSOR,SPACE,BACKSPACE,UTF8,IMG,BPM,VAL,VAR,MACRO_START,
        MACRO_END,LOOP,ENDLOOP,MACRO_VAL,MACRO_VAR,MACRO,COROUTINE,ASSIGN
    )
}

object Operators {
    const val PLUS = "+"
    const val MINUS = "-"
    const val MULTIPLY = "*"
    const val DIVIDE = "/"
    const val POWER = "^"
    const val LOG = "l"
    const val LEFT_PARENTHESIS = "("
    const val RIGHT_PARENTHESIS = ")"
    val ALL_OPERATORS = setOf(
        PLUS,MINUS,MULTIPLY,DIVIDE,POWER,LOG,LEFT_PARENTHESIS,RIGHT_PARENTHESIS
    )
}