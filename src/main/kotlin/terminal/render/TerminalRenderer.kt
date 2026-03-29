package com.lockedfog.kliplayer.terminal.render

import com.lockedfog.kliplayer.config.AnsiCodes

class TerminalRenderer(val width: Int, val height: Int) {
    private val backBuffer = RenderBuffer(width, height)
    private val frontBuffer = RenderBuffer(width, height)

    val canvas: RenderBuffer get() = backBuffer

    fun flush() {
        val outputStream = StringBuilder()

        for (r in 1..height) {
            for (c in 1..width) {
                val backCell = backBuffer.getCell(r,c)!!
                val frontCell = frontBuffer.getCell(r,c)!!

                if (backCell.isWidePlaceholder) continue

                if (!backCell.isVisuallyEqualTo(frontCell)) {
                    outputStream.append(AnsiCodes.cursorMove(r, c))
                    outputStream.append(buildStyleString(backCell))
                    outputStream.append(backCell.char)

                    frontCell.char = backCell.char
                    frontCell.fgColor = backCell.fgColor
                    frontCell.bgColor = backCell.bgColor
                    frontCell.styles.clear()
                    frontCell.styles.addAll(backCell.styles)
                    frontCell.isWidePlaceholder = backCell.isWidePlaceholder
                }
            }
        }

        if (outputStream.isNotEmpty()) {
            outputStream.append(AnsiCodes.PREFIX)
                .append(AnsiCodes.RESET_ALL)
                .append(AnsiCodes.SUFFIX)
            print(outputStream.toString())
            System.out.flush()
        }
    }

    private fun buildStyleString(cell: RenderCell): String {
        val builder = StringBuilder()

        // 1. 状态隔离：首先重置所有样式和颜色，防止被上一个光标的样式污染
        builder.append(AnsiCodes.RESET_ALL)

        // 2. 解析并拼装前景色 (假设颜色格式为 "rrggbb")
        if (cell.fgColor != "default" && cell.fgColor.length == 6) {
            try {
                val r = cell.fgColor.substring(0, 2).toInt(16)
                val g = cell.fgColor.substring(2, 4).toInt(16)
                val b = cell.fgColor.substring(4, 6).toInt(16)
                builder.append(AnsiCodes.foreground(r, g, b))
            } catch (e: NumberFormatException) {
                // 解析失败时忽略，保持默认色
            }
        }

        // 3. 解析并拼装背景色
        if (cell.bgColor != "default" && cell.bgColor.length == 6) {
            try {
                val r = cell.bgColor.substring(0, 2).toInt(16)
                val g = cell.bgColor.substring(2, 4).toInt(16)
                val b = cell.bgColor.substring(4, 6).toInt(16)
                builder.append(AnsiCodes.background(r, g, b))
            } catch (e: NumberFormatException) {
                // 解析失败时忽略
            }
        }

        // 4. 拼装文本样式（加粗、斜体等）
        for (style in cell.styles) {
            builder.append(style.enableString())
        }

        return builder.toString()
    }
}