package com.lockedfog.kliplayer.model

/**
 * 元信息
 */
data class MetaInfo(
    val music: String? = null,
    val script: String? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    companion object {
        fun fromStatement(stmt: MetaStatement): MetaInfo {
            return when (stmt.key.lowercase()) {
                "music" -> MetaInfo(music = stmt.value)
                "script" -> MetaInfo(script = stmt.value)
                "width" -> MetaInfo(width = stmt.value.toIntOrNull())
                "height" -> MetaInfo(height = stmt.value.toIntOrNull())
                else -> MetaInfo()
            }
        }
    }
}
