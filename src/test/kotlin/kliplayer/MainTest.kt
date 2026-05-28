package kliplayer

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {
    @Test
    fun `play with missing audio prints explicit fallback warning`() {
        val script = Files.createTempFile("kliplayer-missing-audio", ".klip")
        Files.writeString(
            script,
            """
            [meta music="missing.mp3"]
            [meta width=4]
            [meta height=2]
            [track one cursor=main z=1 protect=off]
            [00:00.000][mv 1,1]A
            [endtrack]
            """.trimIndent(),
        )

        val originalOut = System.out
        val originalErr = System.err
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(out))
            System.setErr(PrintStream(err))
            assertEquals(0, Main().run(arrayOf("play", script.toString())))
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            Files.deleteIfExists(script)
        }

        assertTrue(err.toString().contains("warning: audio file not found"))
        assertTrue(err.toString().contains("using monotonic no-audio clock"))
    }
}
