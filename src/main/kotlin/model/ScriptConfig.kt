package com.lockedfog.kliplayer.model

/**
 * 脚本配置
 */
data class ScriptConfig(
    val meta: MetaInfo = MetaInfo(),
    val statements: List<Statement> = emptyList()
)
