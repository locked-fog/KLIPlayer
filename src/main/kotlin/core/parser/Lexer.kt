package com.lockedfog.kliplayer.core.parser

import com.lockedfog.kliplayer.config.Keywords
import com.lockedfog.kliplayer.config.Operators
import com.lockedfog.kliplayer.exception.ParseException

class Lexer(private val input: String) {
    private var pos = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()

    fun tokenize(): List<Token> {
        while (pos < input.length) {
            when (input[pos]) {
                ' ' -> {
                    column++
                    pos++
                }
                '/' -> handleComment()
                '\\' -> handleEscapeSequence()
                '[' -> handleBracket()
                else -> handleText()
            }
        }
        tokens.add(Token(TokenType.EOF, "", line, column))
        return tokens
    }

    // private helper functions for handling different token types

    private fun handleEscapeSequence(isInString: Boolean = false): String {
        if (pos + 1 >= input.length) {
            throw ParseException("Unexpected end of input after escape character", line, column)
        }
        val startColumn = column
        val value = when (val nextChar = input[pos + 1]) {
            // special characters
            '[' -> "["
            ']' -> "]"
            // common escape sequences
            'n' -> "\n"
            't' -> "\t"
            '\\' -> "\\"
            else -> throw ParseException("Invalid escape sequence: \\$nextChar", line, column)
        }
        pos += 2
        column += 2
        if (!isInString) {
            tokens.add(Token(TokenType.TEXT, value, line, startColumn))
        }
        return value
    }

    private fun handleBracket() {
        tokens.add(Token(TokenType.LEFT_BRACKET, "[", line, column))
        pos++
        column++
        if (pos >= input.length) {
            throw ParseException("Unterminated bracket content", line, column)
        }
        when (input[pos]) {
            '+' -> handleRelativeTimeOrBeat()
            in '0'..'9' -> handleAbsoluteTimeOrBeat()
            else -> handleBracketContent()
        }
        if (pos >= input.length || input[pos] != ']') {
            throw ParseException("Unterminated bracket content", line, column)
        }
        tokens.add(Token(TokenType.RIGHT_BRACKET, "]", line, column))
        pos++
        column++
    }

    private fun handleRelativeTimeOrBeat() {
        //scan until ']'
        pos++
        column++
        val content = StringBuilder()
        while (pos < input.length && input[pos] != ']') {
            content.append(input[pos])
            pos++
            column++
        }
        if (pos >= input.length) {
            throw ParseException("Unterminated bracket content", line, column)
        }
        tokens.add(Token(TokenType.TIME_REL, content.toString(), line, column - content.length))
    }

    private fun handleAbsoluteTimeOrBeat() {
        val content = StringBuilder()
        while (pos < input.length && input[pos] != ']') {
            content.append(input[pos])
            pos++
            column++
        }
        if (pos >= input.length) {
            throw ParseException("Unterminated bracket content", line, column)
        }
        tokens.add(Token(TokenType.TIME_ABS, content.toString(), line, column - content.length))
    }

    private fun handleBracketContent() {
        val content = StringBuilder()

        fun flushContent() {
            val contentStr = content.toString()
            if (contentStr.isNotEmpty()) {
                when {
                    contentStr.startsWith("@@") -> {
                        tokens.add(Token(TokenType.KEYWORD, "@@", line, column - contentStr.length))
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr.substring(2), line, column - content.length + 2))
                    }
                    contentStr.startsWith("@") -> {
                        tokens.add(Token(TokenType.KEYWORD, "@", line, column - contentStr.length))
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr.substring(1), line, column - content.length + 1))
                    }
                    contentStr.startsWith("=") -> {
                        tokens.add(Token(TokenType.KEYWORD, "=", line, column - contentStr.length))
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr.substring(1), line, column - content.length + 1))
                        handleExpression()
                    }
                    contentStr.startsWith("#") -> {
                        tokens.add(Token(TokenType.KEYWORD, "#", line, column - contentStr.length))
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr.substring(1), line, column - content.length + 1))
                    }
                    contentStr.startsWith("$") -> {
                        tokens.add(Token(TokenType.KEYWORD, "$", line, column - contentStr.length))
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr.substring(1), line, column - content.length + 1))
                    }
                    contentStr in Keywords.ALL_KEYS -> {
                        tokens.add(Token(TokenType.KEYWORD, contentStr, line, column - contentStr.length))
                    }
                    contentStr.matches("^[0-9A-Fa-f]{6}$".toRegex()) -> {
                        tokens.add(Token(TokenType.COLOR, contentStr, line, column - contentStr.length))
                    }
                    contentStr.matches("^-?\\d+(\\.\\d+)?$".toRegex()) -> {
                        tokens.add(Token(TokenType.NUMBER, contentStr, line, column - contentStr.length))
                    }
                    else -> {
                        tokens.add(Token(TokenType.IDENTIFIER, contentStr, line, column - contentStr.length))
                    }
                }
            }
            content.clear()
        }

        while (pos < input.length && input[pos] != ']') {
            when (input[pos]) {
                ' ', ',' -> {
                    flushContent()
                    pos++
                    column++
                }
                '[' -> {
                    pos++
                    column++
                    val subcontent = StringBuilder()
                    while (pos < input.length && input[pos] != ']') {
                        subcontent.append(input[pos])
                        pos++
                        column++
                    }
                    if (pos >= input.length) {
                        throw ParseException("Unterminated bracket content", line, column)
                    }
                    tokens.add(Token(TokenType.IDENTIFIER, subcontent.toString(), line, column - subcontent.length))
                    pos++
                    column++
                }
                '"' -> handleString()
                else -> {
                    content.append(input[pos])
                    pos++
                    column++
                }
            }
        }

        if (pos >= input.length) {
            throw ParseException("Unterminated bracket content", line, column)
        }

        flushContent()
    }

    private fun handleString() {
        val startColumn = column
        pos++
        column++
        val content = StringBuilder()
        while (pos < input.length && input[pos] != '"') {
            if (input[pos] == '\\') {
                content.append(handleEscapeSequence(isInString = true))
            } else {
                content.append(input[pos])
                pos++
                column++
            }
        }
        if (pos >= input.length) {
            throw ParseException("Unterminated string literal", line, column)
        }
        pos++
        column++
        tokens.add(Token(TokenType.STRING, content.toString(), line, startColumn))
    }

    private fun handleComment() {
        if (pos + 1 < input.length && input[pos + 1] == '/') {
            while (pos < input.length && input[pos] != '\n') {
                pos++
                column++
            }
            if (pos < input.length && input[pos] == '\n') {
                tokens.add(Token(TokenType.NEWLINE, "\n", line, column))
                column = 1
                pos++
                line++
            }
        } else {
            handleText()
        }
    }

    private fun handleText() {
        if (input[pos] == '\n') {
            tokens.add(Token(TokenType.NEWLINE, "\n", line, column))
            column = 1
            pos++
            line++
        } else {
            tokens.add(Token(TokenType.TEXT,input[pos].toString(), line, column))
            column++
            pos++
        }
    }

    private fun handleExpression() {
        val content = StringBuilder()
        pos++
        column++
        while (pos < input.length && input[pos] != ']') {
            when {
                (input[pos].toString() in Operators.ALL_OPERATORS) -> {
                    if (content.isNotEmpty()) {
                        tokens.add(Token(TokenType.NUMBER, content.toString(), line, column-content.length))
                    }
                    tokens.add(Token(TokenType.OPERATOR, input[pos].toString(), line, column))
                    pos++
                    column++
                    content.clear()
                }
                (input[pos] == '[') -> {
                    if (content.isNotEmpty()) {
                        throw ParseException("Unexpected content before nested bracket in expression", line, column)
                    }
                    pos++
                    column++
                    val subContent = StringBuilder()
                    while (pos < input.length && input[pos] != ']') {
                        subContent.append(input[pos])
                        pos++
                        column++
                    }
                    if (pos >= input.length) {
                        throw ParseException("Unterminated bracket in expression", line, column)
                    }
                    tokens.add(Token(TokenType.IDENTIFIER, subContent.toString(), line, column-subContent.length))
                    pos++
                    column++
                }
                (input[pos] == ' ') -> {
                    pos++
                    column++
                }
                else -> {
                    content.append(input[pos])
                    pos++
                    column++
                }
            }
        }
        if (content.isNotEmpty()) {
            tokens.add(Token(TokenType.NUMBER, content.toString(), line, column - content.length))
        }
    }
}
