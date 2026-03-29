package com.lockedfog.kliplayer.terminal.render

import com.lockedfog.kliplayer.config.StyleType

data class RenderCell(
    var char: String = " ",
    var fgColor: String = "default",
    var bgColor: String = "default",
    var styles: MutableSet<StyleType> = emptySet<StyleType>().toMutableSet(),
    var level: Int = Int.MAX_VALUE,
    var isProtected: Boolean = false,
    var isWidePlaceholder: Boolean = false
) {
    fun reset() {
        char = " "
        fgColor = "default"
        bgColor = "default"
        styles.clear()
        level = Int.MAX_VALUE
        isProtected = false
        isWidePlaceholder = false
    }

    fun isVisuallyEqualTo(other: RenderCell): Boolean {
        return this.char == other.char &&
                this.fgColor == other.fgColor &&
                this.bgColor == other.bgColor &&
                this.styles == other.styles &&
                this.isWidePlaceholder == other.isWidePlaceholder
    }
}