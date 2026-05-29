package kliplayer

import java.io.Flushable

class TerminalRenderer(
    private val width: Int,
    private val height: Int,
    private val out: Appendable = System.out,
    private val mask: ProtectionMask = ProtectionMask(width, height),
) {
    private val cursors = mutableMapOf<String, CursorState>()
    private val eraseStyle = RenderStyle()
    private var physicalStyle: RenderStyle? = null

    fun render(event: Event) {
        val cursor = cursors.getOrPut(event.cursorId) { CursorState() }
        for (op in event.ops) {
            when (op) {
                is Move -> {
                    cursor.row = op.row
                    cursor.col = op.col
                }
                is Foreground -> cursor.style = cursor.style.copy(foregroundRgb = op.rgb)
                is Background -> cursor.style = cursor.style.copy(backgroundRgb = op.rgb)
                is Style -> cursor.style = cursor.style.apply(op)
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
        physicalStyle = RenderStyle()
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
            ensurePhysicalStyle(cursor.style)
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
                ensurePhysicalStyle(eraseStyle)
                movePhysical(row, col)
                out.append(' ')
            }
        }
    }

    private fun clear(writerZ: Int) {
        for (row in 1..height) {
            for (col in 1..width) {
                if (mask.clear(row, col, writerZ)) {
                    ensurePhysicalStyle(eraseStyle)
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

    private fun ensurePhysicalStyle(target: RenderStyle) {
        val current = physicalStyle
        if (current == target) return

        if (current == null || current.foregroundRgb != target.foregroundRgb) {
            out.append(if (target.foregroundRgb == null) "\u001b[39m" else rgbAnsi(38, target.foregroundRgb))
        }
        if (current == null || current.backgroundRgb != target.backgroundRgb) {
            out.append(if (target.backgroundRgb == null) "\u001b[49m" else rgbAnsi(48, target.backgroundRgb))
        }
        if (current == null || current.bold != target.bold) {
            sgr(if (target.bold) 1 else 22)
        }
        if (current == null || current.italic != target.italic) {
            sgr(if (target.italic) 3 else 23)
        }
        if (current == null || current.underline != target.underline) {
            sgr(if (target.underline) 4 else 24)
        }
        if (current == null || current.strikeline != target.strikeline) {
            sgr(if (target.strikeline) 9 else 29)
        }
        physicalStyle = target
    }

    private fun sgr(code: Int) {
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

    private data class CursorState(
        var row: Int = 1,
        var col: Int = 1,
        var style: RenderStyle = RenderStyle(),
    )

    private data class RenderStyle(
        val foregroundRgb: String? = null,
        val backgroundRgb: String? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val strikeline: Boolean = false,
    ) {
        fun apply(style: Style): RenderStyle =
            when (style.name) {
                null -> copy(bold = false, italic = false, underline = false, strikeline = false)
                "bold" -> copy(bold = style.enabled == true)
                "italic" -> copy(italic = style.enabled == true)
                "underline" -> copy(underline = style.enabled == true)
                "strikeline" -> copy(strikeline = style.enabled == true)
                else -> this
            }
    }
}
