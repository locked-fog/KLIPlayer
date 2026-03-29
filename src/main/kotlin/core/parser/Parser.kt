package com.lockedfog.kliplayer.core.parser

import com.lockedfog.kliplayer.exception.ParseException
import com.lockedfog.kliplayer.model.*
import com.lockedfog.kliplayer.utils.TimeFormat

class Parser (private val tokens: List<Token>) {
    private val macros = mutableMapOf<String, MacroTemplate>()

    fun parse(): ScriptConfig {
        val mainTokens = extractMacros(tokens)

        val flatTokens = expand(mainTokens, emptyMap())

        return buildTimeline(flatTokens)
    }

    data class MacroTemplate(
        val name: String,
        val paramNames: List<String>,
        val bodyTokens: List<Token>
    )

    private fun extractMacros(input: List<Token>): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        while (i < input.size) {
            val token = input[i]
            if (token.type == TokenType.LEFT_BRACKET &&
                i + 2 < input.size &&
                input[i + 1].type == TokenType.KEYWORD &&
                input[i + 1].value == "#") {

                val macroName = input[i+2].value
                val params = mutableListOf<String>()
                var j = i+3

                while (input[j].type != TokenType.RIGHT_BRACKET) {
                    if (input[j].type == TokenType.IDENTIFIER) {
                        params.add(input[j].value)
                    }
                    j++
                }
                j++

                val body = mutableListOf<Token>()
                while (!(input[j].type == TokenType.LEFT_BRACKET && input[j+1].value == "endmacro")) {
                    body.add(input[j])
                    j++
                }
                while (input[j].type != TokenType.RIGHT_BRACKET) j++

                macros[macroName] = MacroTemplate(macroName, params, body)
                i = j + 1
            } else {
                result.add(token)
                i++
            }
        }
        return result
    }

    fun expand(tokens: List<Token>, args: Map<String,String>): List<Token> {
        val result = mutableListOf<Token>()
        val localArgs = args.toMutableMap()
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]
            when {
                (token.type == TokenType.LEFT_BRACKET && i + 2 < tokens.size &&
                tokens[i + 1].type == TokenType.KEYWORD && tokens[i + 1].value == "@"
                ) -> {
                    val valName = tokens[i + 2].value
                    localArgs[valName] = tokens[i + 3].value
                    i += 5
                }
                (token.type == TokenType.IDENTIFIER && localArgs.containsKey(token.value)) -> {
                    val paramValue = localArgs[token.value]!!
                    result.add(Token(TokenType.IDENTIFIER,paramValue,token.line,token.column))
                    i++
                }
                (token.type == TokenType.IDENTIFIER && args.containsKey(token.value)) -> {
                    val paramValue = args[token.value]!!
                    result.add(Token(TokenType.IDENTIFIER, paramValue, token.line, token.column))
                    i++
                }
                (token.type == TokenType.LEFT_BRACKET &&
                i+1< tokens.size &&
                tokens[i+1].type == TokenType.KEYWORD &&
                tokens[i+1].value == "loop") -> {
                    val countToken = tokens[i+2]
                    var countStr = countToken.value

                    if (countToken.type == TokenType.IDENTIFIER && args.containsKey(countToken.value)) {
                        countStr = args[countToken.value]!!
                    }
                    val count = countStr.toIntOrNull() ?: throw ParseException(
                        "Invalid loop count: $countStr",
                        countToken.line,
                        countToken.column
                    )

                    var j = i + 3
                    while (j < tokens.size && tokens[j].type != TokenType.RIGHT_BRACKET) j++
                    j++

                    val loopBody = mutableListOf<Token>()
                    var depth = 1
                    while (j < tokens.size && depth > 0) {
                        if (tokens[j].type == TokenType.LEFT_BRACKET &&
                            j + 1 < tokens.size) {
                            if (tokens[j+1].type == TokenType.KEYWORD &&
                                tokens[j+1].value == "loop") depth++
                            if (tokens[j+1].type == TokenType.KEYWORD &&
                                tokens[j+1].value == "endloop") {
                                depth--
                                if (depth == 0) break
                            }
                        }
                        if (depth > 0) loopBody.add(tokens[j])
                        j++
                    }
                    while (j < tokens.size && tokens[j].type != TokenType.RIGHT_BRACKET) j++
                    j++

                    val expandedBody = expand(loopBody,localArgs)

                    for (k in 0 until count) {
                        result.addAll(expandedBody)
                    }

                    i = j
                }
                (token.type == TokenType.LEFT_BRACKET && i+1<tokens.size) -> {
                    val nextToken = tokens[i + 1]
                    var isCoroutine = false
                    var macroNameTokenIndex = i + 1

                    // 检测是否为协程调用 '$'
                    if (nextToken.type == TokenType.KEYWORD && nextToken.value == "$") {
                        isCoroutine = true
                        macroNameTokenIndex = i + 2
                    }

                    val macroNameToken = tokens[macroNameTokenIndex]
                    // 如果是一个已知的宏调用
                    if (macroNameToken.type == TokenType.IDENTIFIER && macros.containsKey(macroNameToken.value)) {
                        val template = macros[macroNameToken.value]!!

                        // 收集调用时传入的参数
                        val callArgs = mutableListOf<String>()
                        var j = macroNameTokenIndex + 1
                        while (j < tokens.size && tokens[j].type != TokenType.RIGHT_BRACKET) {
                            val argToken = tokens[j]
                            // 过滤掉逗号和空格引起的空白 Token
                            if (argToken.type != TokenType.TEXT && argToken.value.isNotBlank()) {
                                // 如果传入的实参也是一个变量，先做求值替换
                                val argValue =
                                    if (argToken.type == TokenType.IDENTIFIER && args.containsKey(argToken.value)) {
                                        args[argToken.value]!!
                                    } else {
                                        argToken.value
                                    }
                                callArgs.add(argValue)
                            }
                            j++
                        }
                        j++ // 跳过 ']'

                        // 构建新的参数作用域 (newArgs)
                        val newArgs = localArgs
                        var argOffset = 0
                        var cursorId = ""

                        // 如果是协程，第一个参数必定是 cursorId
                        if (isCoroutine && callArgs.isNotEmpty()) {
                            cursorId = callArgs[0]
                            argOffset = 1
                        }

                        // 将传入的值绑定到宏定义的参数名上
                        for (k in template.paramNames.indices) {
                            if (k + argOffset < callArgs.size) {
                                newArgs[template.paramNames[k]] = callArgs[k + argOffset]
                            } else {
                                newArgs[template.paramNames[k]] = "" // 缺少参数则给空串
                            }
                        }

                        // 在新的参数作用域下递归展开宏内部内容
                        val expandedMacro = expand(template.bodyTokens, newArgs)

                        if (isCoroutine) {
                            // 协程封装：插入协程起点标记
                            result.add(Token(TokenType.COROUTINE_START, "START", token.line, token.column))

                            if (cursorId.isNotEmpty()) {
                                // 按照需求，隐式生成 [cursor <cursorid>] 语句
                                result.add(Token(TokenType.LEFT_BRACKET, "[", token.line, token.column))
                                result.add(Token(TokenType.KEYWORD, "cursor", token.line, token.column))
                                result.add(Token(TokenType.IDENTIFIER, cursorId, token.line, token.column))
                                result.add(Token(TokenType.RIGHT_BRACKET, "]", token.line, token.column))
                            }

                            result.addAll(expandedMacro)
                            // 协程封装：插入协程终点标记
                            result.add(Token(TokenType.COROUTINE_END, "END", token.line, token.column))
                        } else {
                            result.addAll(expandedMacro)
                        }
                        i = j
                    } else if (localArgs.containsKey(macroNameToken.value)) {
                        val macroValue = localArgs[macroNameToken.value]!!
                        if(macroValue.matches("^-?\\d+(\\.\\d+)?$".toRegex())){
                            result.add(Token(TokenType.NUMBER,macroValue,token.line, token.column))
                        } else {
                            result.add(Token(TokenType.TEXT, macroValue,token.line, token.column))
                        }
                        i += 3
                    } else {
                        result.add(token)
                        i++
                    }
                }
                else -> {
                    result.add(token)
                    i++
                }
            }
        }
        return result
    }

    private fun buildTimeline(flatTokens: List<Token>): ScriptConfig {
        // 临时存储所有的 (绝对时间戳 -> 控制语句) 的键值对
        val events = mutableListOf<Pair<Long, ControlStatement>>()

        // 元信息存储
        var music: String? = null
        var script: String? = null
        var width: Int? = null
        var height: Int? = null

        // 时间轴状态
        var currentTime = 0L
        var currentBpm: Double? = null
        var bpmStartTime: Long? = null

        // 协程时间栈：用于保护主时间轴不受协程内部相对时间的污染
        val timeStack = ArrayDeque<Long>()

        var i = 0
        while (i < flatTokens.size) {
            val token = flatTokens[i]

            when (token.type) {
                // 1. 协程边界标记 (由阶段 2 埋入)
                TokenType.COROUTINE_START -> {
                    timeStack.addLast(currentTime) // 压入当前主时间线
                    i++
                }
                TokenType.COROUTINE_END -> {
                    currentTime = timeStack.removeLast() // 弹出并恢复主时间线
                    i++
                }

                // 2. 括号内的控制语句及时间戳
                TokenType.LEFT_BRACKET -> {
                    // 寻找匹配的右括号，框定当前标签的内容
                    var j = i + 1
                    while (j < flatTokens.size && flatTokens[j].type != TokenType.RIGHT_BRACKET) {
                        j++
                    }

                    // 提取括号内部的 Token (不含左右括号)
                    val content = flatTokens.subList(i + 1, j)

                    if (content.isNotEmpty()) {
                        val first = content[0]
                        when (first.type) {
                            // 2.1 更新当前绝对时间
                            TokenType.TIME_ABS, TokenType.TIME_REL -> {
                                try {
                                    currentTime = TimeFormat.timeFormat(
                                        time = first.value,
                                        timeLast = currentTime,
                                        bpm = currentBpm,
                                        bpmStart = bpmStartTime
                                    )
                                } catch (e: ParseException) {
                                    throw ParseException("Invalid time format [${content[0].line}:${content[0].column}] with error content: \n ${e.message}")
                                }
                            }

                            // 2.2 处理各种控制指令
                            TokenType.KEYWORD -> {
                                // 提取有意义的实参（过滤掉逗号和纯空格，但保留 '=' 给 style 使用）
                                val rawArgs = content.subList(1, content.size)
                                    .filter { it.type != TokenType.TEXT || it.value.isNotBlank() }

                                val argStrings = rawArgs
                                    .map { it.value }

                                when (first.value.lowercase()) {
                                    "meta" -> {
                                        val keyToken = rawArgs.firstOrNull { it.type == TokenType.IDENTIFIER }
                                        val valToken = rawArgs.firstOrNull { it.type == TokenType.STRING || it.type == TokenType.NUMBER }
                                        if (keyToken != null && valToken != null) {
                                            when (keyToken.value.lowercase()) {
                                                "music" -> music = valToken.value
                                                "script" -> script = valToken.value
                                                "width" -> width = valToken.value.toIntOrNull()
                                                "height" -> height = valToken.value.toIntOrNull()
                                            }
                                        }
                                    }
                                    "bpm" -> {
                                        val bpmVal = argStrings.firstOrNull()?.toDoubleOrNull()
                                        if (bpmVal != null) {
                                            currentBpm = bpmVal
                                            bpmStartTime = currentTime
                                            events.add(currentTime to SetBpm(bpmVal.toInt()))
                                        }
                                    }
                                    "newcursor" -> argStrings.firstOrNull()?.let { events.add(currentTime to NewCursor(it)) }
                                    "cursor" -> argStrings.firstOrNull()?.let { events.add(currentTime to SwitchCursor(it)) }
                                    "mv" -> {
                                        if (argStrings.size >= 2) {
                                            val row = argStrings[0].toIntOrNull() ?: 1
                                            val col = argStrings[1].toIntOrNull() ?: 1
                                            events.add(currentTime to MoveCursor(row, col))
                                        }
                                    }
                                    "hide" -> events.add(currentTime to HideCursor())
                                    "show" -> events.add(currentTime to ShowCursor())
                                    "color" -> argStrings.firstOrNull()?.let {
                                        if (it.lowercase() == "default") events.add(currentTime to ResetColor())
                                        else events.add(currentTime to SetColor(it))
                                    }
                                    "background" -> argStrings.firstOrNull()?.let {
                                        if (it.lowercase() == "default") events.add(currentTime to ResetBackground())
                                        else events.add(currentTime to SetBackground(it))
                                    }
                                    "style" -> {
                                        if (argStrings.firstOrNull()?.lowercase() == "default") {
                                            events.add(currentTime to ResetStyle())
                                        } else {
                                            // 处理如 [style bold=on, italic=off] 的语法
                                            var k = 0
                                            while (k < argStrings.size) {
                                                val styleName = argStrings[k]
                                                var enabled = true
                                                if (k + 1 < argStrings.size) {
                                                    enabled = argStrings[k + 1].lowercase() == "on"
                                                    k += 2
                                                } else {
                                                    k += 1
                                                }
                                                events.add(currentTime to SetStyle(styleName, enabled))
                                            }
                                        }
                                    }
                                    "level" -> argStrings.firstOrNull()?.toIntOrNull()?.let { events.add(currentTime to SetLevel(it)) }
                                    "protect" -> argStrings.firstOrNull()?.let { events.add(currentTime to SetProtect(it.lowercase() == "on")) }
                                    "clean" -> events.add(currentTime to CleanScreen())
                                    "cleanline" -> events.add(currentTime to CleanLine())
                                    "newline" -> events.add(currentTime to NewLine())
                                    "delcursor" -> argStrings.firstOrNull()?.let { events.add(currentTime to DeleteCursor(it)) }
                                    "space" -> {
                                        val count = argStrings.firstOrNull()?.toIntOrNull() ?: 1
                                        events.add(currentTime to OutputSpace(count))
                                    }
                                    "backspace" -> {
                                        val count = argStrings.firstOrNull()?.toIntOrNull() ?: 1
                                        events.add(currentTime to Backspace(count))
                                    }
                                    "utf-8" -> argStrings.firstOrNull()?.toIntOrNull()?.let { events.add(currentTime to OutputUtf8(it)) }
                                    "img" -> {
                                        val strToken = rawArgs.firstOrNull { it.type == TokenType.STRING }
                                        val nums = rawArgs.filter { it.type == TokenType.NUMBER }.mapNotNull { it.value.toIntOrNull() }
                                        if (nums.size >= 4 && strToken != null) {
                                            events.add(currentTime to OutputImage(nums[0], nums[1], nums[2], nums[3], strToken.value))
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                    i = j + 1 // 跳过闭合的右括号 ']'
                }

                TokenType.IDENTIFIER -> {
                    events.add(currentTime to OutputText(token.value))
                    i++
                }

                // 3. 提取普通文本（非脚本格式物理换行）
                TokenType.TEXT -> {
                    events.add(currentTime to OutputText(token.value))
                    i++
                }

                // 脚本文件的物理换行(NEWLINE) 直接被忽略，不影响输出
                // 终端实际的换行是通过 TEXT("\n") [经过转义的\n] 或者是 [newline] 实现的
                TokenType.NEWLINE -> {
                    i++
                }

                else -> i++
            }
        }

        events.sortBy { it.first }

        val timelineStatements = mutableListOf<TimelineStatement>()
        if (events.isNotEmpty()) {
            var currentGroupTime = events.first().first
            var currentGroupActions = mutableListOf<ControlStatement>()

            for ((time, action) in events) {
                if (time != currentGroupTime) {
                    timelineStatements.add(TimelineStatement(currentGroupTime, currentGroupActions))
                    currentGroupTime = time
                    currentGroupActions = mutableListOf()
                }
                currentGroupActions.add(action)
            }
            // 不要忘记把最后一组加入进去
            timelineStatements.add(TimelineStatement(currentGroupTime, currentGroupActions))
        }

        // 最终组装交付给执行引擎
        return ScriptConfig(
            meta = MetaInfo(music, script, width, height),
            statements = timelineStatements
        )
    }

}