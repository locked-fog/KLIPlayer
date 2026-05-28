package kliplayer

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalRendererTest {
    @Test
    fun `renders movement colors styles cursor visibility and text as ansi`() {
        val out = StringBuilder()
        val renderer = TerminalRenderer(width = 20, height = 5, out = out)

        renderer.render(
            event(
                z = 10,
                ops = listOf(
                    Move(2, 3),
                    Foreground("ff0055"),
                    Background("101010"),
                    Style("bold", true),
                    HideCursor,
                    ShowCursor,
                    Text("A"),
                ),
            ),
        )

        val rendered = out.toString()
        assertContains(rendered, "\u001b[38;2;255;0;85m")
        assertContains(rendered, "\u001b[48;2;16;16;16m")
        assertContains(rendered, "\u001b[1m")
        assertContains(rendered, "\u001b[?25l")
        assertContains(rendered, "\u001b[?25h")
        assertTrue(rendered.endsWith("\u001b[2;3HA"))
    }

    @Test
    fun `lower z text cannot overwrite higher protected wide text`() {
        val out = StringBuilder()
        val mask = ProtectionMask(width = 10, height = 2)
        val renderer = TerminalRenderer(width = 10, height = 2, out = out, mask = mask)

        renderer.render(event(z = 100, protect = true, ops = listOf(Move(1, 1), Text("熱"))))
        val afterProtectedWrite = out.length
        renderer.render(event(z = 20, ops = listOf(Move(1, 1), Text("X"))))

        assertEquals(afterProtectedWrite, out.length)
        assertEquals(100, mask.protectedAt(1, 1))
        assertEquals(100, mask.protectedAt(1, 2))
    }

    @Test
    fun `same z can overwrite protected text`() {
        val out = StringBuilder()
        val mask = ProtectionMask(width = 10, height = 2)
        val renderer = TerminalRenderer(width = 10, height = 2, out = out, mask = mask)

        renderer.render(event(z = 100, protect = true, ops = listOf(Move(1, 1), Text("A"))))
        renderer.render(event(z = 100, ops = listOf(Move(1, 1), Text("B"))))

        assertTrue(out.toString().endsWith("\u001b[1;1HB"))
    }

    @Test
    fun `cleanline and clear respect protection mask`() {
        val out = StringBuilder()
        val mask = ProtectionMask(width = 3, height = 2)
        val renderer = TerminalRenderer(width = 3, height = 2, out = out, mask = mask)

        renderer.render(event(z = 100, protect = true, ops = listOf(Move(1, 1), Text("A"))))
        out.clear()
        renderer.render(event(z = 20, ops = listOf(Move(1, 1), CleanLine)))

        assertFalse(out.toString().contains("\u001b[1;1H "))
        assertContains(out.toString(), "\u001b[1;2H ")
        assertEquals(100, mask.protectedAt(1, 1))

        out.clear()
        renderer.render(event(z = 100, ops = listOf(Clear)))

        assertContains(out.toString(), "\u001b[1;1H ")
        assertEquals(ProtectionMask.UNPROTECTED, mask.protectedAt(1, 1))
    }

    @Test
    fun `newline advances logical cursor without ansi output until text`() {
        val out = StringBuilder()
        val renderer = TerminalRenderer(width = 10, height = 3, out = out)

        renderer.render(event(ops = listOf(Move(1, 3), Text("A"), Newline, Text("B"))))

        assertTrue(out.toString().endsWith("\u001b[1;3HA\u001b[2;1HB"))
    }

    private fun event(
        z: Int = 1,
        protect: Boolean = false,
        ops: List<Op>,
    ): Event =
        Event(
            timeMs = 0L,
            order = 0L,
            cursorId = "test",
            z = z,
            protect = protect,
            ops = ops,
            sourceLine = 1,
            source = "test",
        )
}
