package com.lockedfog.kliplayer.terminal.render

import com.lockedfog.kliplayer.config.StyleType
import com.lockedfog.kliplayer.utils.FullwidthCharacterUtils

class RenderBuffer(val width: Int, val height: Int) {
    private val grid: Array<Array<RenderCell>> = Array(height) {
        Array(width) {
            RenderCell()
        }
    }

    fun clear() {
        for(r in 0 until height) {
            for(c in 0 until width) {
                grid[r][c].reset()
            }
        }
    }

    fun  getCell(row: Int, col: Int): RenderCell? {
        val r = row-1
        val c = col-1
        if (r !in 0 until width || c !in 0 until height) return null
        return grid[r][c]
    }

    /**
     * @return 返回真实占用的视觉宽度，失败返回0
     */
    fun writeChar(
        row: Int, col: Int, charStr: String,
        fg: String, bg: String, styles: Set<StyleType>,
        level: Int, isProtected: Boolean
    ) : Int {
        val r = row-1
        val c = col-1
        if (r !in 0 until height && c !in 0 until width) return 0

        val targetCell = grid[r][c]

        if (targetCell.isProtected && level > targetCell.level) {
            return 0
        }

        val codePoint = charStr.codePointAt(0)
        val charWidth = FullwidthCharacterUtils.width(codePoint)

        if (charWidth == 2) {
            if (c + 1 >= width) return 0
            val rightCell = grid[r][c+1]
            if (rightCell.isProtected && level > rightCell.level) return 0

            rightCell.reset()
            rightCell.isWidePlaceholder = true
            rightCell.level = level
            rightCell.isProtected = isProtected
        }

        targetCell.char = charStr
        targetCell.fgColor = fg
        targetCell.bgColor = bg
        targetCell.styles.clear()
        targetCell.styles.addAll(styles)
        targetCell.level = level
        targetCell.isProtected = isProtected
        targetCell.isWidePlaceholder = false

        return charWidth
    }
}