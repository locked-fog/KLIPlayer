package com.lockedfog.kliplayer.core.parser

data class Token(
    val type: TokenType,
    val value: String,
    val line: Int,
    val column: Int
)