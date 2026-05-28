package kliplayer

import java.io.Flushable

class TerminalRenderer(
    private val width: Int,
    private val height: Int,
    private val out: Appendable = System.out,
    private val mask: ProtectionMask = ProtectionMask(width, height),
) {
    private val cursors = mutableMapOf<String, CursorState>()

    fun render(event: Event) {
        val cursor = cursors.getOrPut(event.cursorId) { CursorState() }
        for (op in event.ops) {
            when (op) {
                is Move -> {
                    cursor.row = op.row
                    cursor.col = op.col
                }
                is Foreground -> out.append(if (op.rgb == null) "\u001b[39m" else rgbAnsi(38, op.rgb))
                is Background -> out.append(if (op.rgb == null) "\u001b[49m" else rgbAnsi(48, op.rgb))
                is Style -> applyStyle(op)
                is Text -> writeText(cursor, op.value, event)
                is Space -> repeat(op.count) { writeCells(cursor, " ", 1, event) }
                Newline -> {
                    cursor.row += 1
                    cursor.col = 1
                }
                CleanLine -> cleanLine(cursor.row, event.z)
                Clear -> clear(event.z)
                HideCursor -> out.append("\u001b[?25l")
                ShowCursor -> out.append("\u001b[?25h")
            }
        }
        flush()
    }

    fun restore() {
        out.append("\u001b[0m\u001b[39m\u001b[49m\u001b[?25h\n")
        flush()
    }

    private fun writeText(cursor: CursorState, value: String, event: Event) {
        val iterator = value.codePoints().iterator()
        while (iterator.hasNext()) {
            val codePoint = iterator.nextInt()
            val displayWidth = WcWidth.ofCodePoint(codePoint)
            if (displayWidth <= 0) continue
            writeCells(cursor, String(Character.toChars(codePoint)), displayWidth, event)
        }
    }

    private fun writeCells(cursor: CursorState, text: String, displayWidth: Int, event: Event) {
        if (mask.canWriteCells(cursor.row, cursor.col, displayWidth, event.z)) {
            movePhysical(cursor.row, cursor.col)
            out.append(text)
            if (event.protect) {
                mask.mark(cursor.row, cursor.col, displayWidth, event.z)
            }
        }
        cursor.col += displayWidth
    }

    private fun cleanLine(row: Int, writerZ: Int) {
        for (col in 1..width) {
            if (mask.clear(row, col, writerZ)) {
                movePhysical(row, col)
                out.append(' ')
            }
        }
    }

    private fun clear(writerZ: Int) {
        for (row in 1..height) {
            for (col in 1..width) {
                if (mask.clear(row, col, writerZ)) {
                    movePhysical(row, col)
                    out.append(' ')
                }
            }
        }
    }

    private fun movePhysical(row: Int, col: Int) {
        if (row in 1..height && col in 1..width) {
            out.append("\u001b[").append(row.toString()).append(';').append(col.toString()).append('H')
        }
    }

    private fun applyStyle(style: Style) {
        if (style.name == null) {
            out.append("\u001b[0m")
            return
        }
        val code = when (style.name) {
            "bold" -> if (style.enabled == true) 1 else 22
            "italic" -> if (style.enabled == true) 3 else 23
            "underline" -> if (style.enabled == true) 4 else 24
            "strikeline" -> if (style.enabled == true) 9 else 29
            else -> return
        }
        out.append("\u001b[").append(code.toString()).append('m')
    }

    private fun rgbAnsi(prefix: Int, rgb: String): String {
        val r = rgb.substring(0, 2).toInt(16)
        val g = rgb.substring(2, 4).toInt(16)
        val b = rgb.substring(4, 6).toInt(16)
        return "\u001b[$prefix;2;$r;$g;${b}m"
    }

    private fun flush() {
        if (out is Flushable) out.flush()
    }

    private data class CursorState(var row: Int = 1, var col: Int = 1)
}
