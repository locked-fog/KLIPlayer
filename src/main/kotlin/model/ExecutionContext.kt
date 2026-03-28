package com.lockedfog.kliplayer.model

/**
 * 执行上下文
 */
class ExecutionContext(
    val config: ScriptConfig,
    var currentCursorId: String = "cursor0",
    var currentBpm: Int? = null,
    var bpmStartTime: Long? = null
) {
    private val macros = mutableMapOf<String, MacroDefinition>()
    private val constants = mutableMapOf<String, String>()
    private val variables = mutableMapOf<String, Int>()

    fun addMacro(name: String, definition: MacroDefinition) {
        macros[name] = definition
    }

    fun getMacro(name: String): MacroDefinition? = macros[name]

    fun addConstant(name: String, value: String) {
        constants[name] = value
    }

    fun getConstant(name: String): String? = constants[name]

    fun setVariable(name: String, value: Int) {
        variables[name] = value
    }

    fun getVariable(name: String): Int? = variables[name]
}
