package com.lockedfog.kliplayer.model

/**
 * 控制语句类型
 */
sealed class ControlStatement

// ===== 光标控制 =====

class NewCursor(val id: String) : ControlStatement()
class SwitchCursor(val id: String) : ControlStatement()
class MoveCursor(val row: Int, val col: Int) : ControlStatement()
class HideCursor : ControlStatement()
class ShowCursor : ControlStatement()
class SetColor(val color: String) : ControlStatement()
class ResetColor : ControlStatement()
class SetBackground(val color: String) : ControlStatement()
class ResetBackground : ControlStatement()
class SetStyle(val style: String, val enabled: Boolean) : ControlStatement()
class ResetStyle : ControlStatement()
class SetLevel(val level: Int) : ControlStatement()
class SetProtect(val enabled: Boolean) : ControlStatement()
class CleanScreen : ControlStatement()
class CleanLine : ControlStatement()
class NewLine : ControlStatement()
class DeleteCursor(val id: String) : ControlStatement()

// ===== 内容输出 =====

class OutputText(val text: String) : ControlStatement()
class OutputSpace(val count: Int = 1) : ControlStatement()
class Backspace(val count: Int = 1) : ControlStatement()
class OutputUtf8(val code: Int) : ControlStatement()
class OutputImage(
    val x1: Int, val y1: Int,
    val x2: Int, val y2: Int,
    val path: String
) : ControlStatement()

// ===== BPM 设置 =====

class SetBpm(val bpm: Int) : ControlStatement()

