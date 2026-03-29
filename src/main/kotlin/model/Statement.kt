package com.lockedfog.kliplayer.model

/**
 * 语句基类
 */
sealed class Statement

/**
 * 元信息语句
 */
class MetaStatement(
    val key: String,
    val value: String
) : Statement()

/**
 * 时间轴语句
 * @param time 绝对时间（毫秒）
 */
class TimelineStatement(
    val time: Long,
    val statements: List<ControlStatement>
) : Statement()

/**
 * 宏定义语句
 */
class MacroDefinition(
    val name: String,
    val parameters: List<MacroParameter>,
    val statements: List<Statement>
) : Statement()

/**
 * 宏参数
 */
class MacroParameter(
    val name: String,
    val value: String
) : Statement()


