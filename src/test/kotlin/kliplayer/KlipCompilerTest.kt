package kliplayer

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KlipCompilerTest {
    @Test
    fun `absolute anchor relative and fraction beat times compile`() {
        val timeline = compile(
            """
            [anchor intro 00:01.000 bpm=120]
            [track lyrics cursor=main z=100 protect=on]
            [intro+1/2b][mv 1,1]A
            [+1b][mv 1,2]B
            [00:05.000][mv 1,3]C
            [endtrack]
            """.trimIndent(),
        )

        assertEquals(listOf(1250L, 1750L, 5000L), timeline.events.map { it.timeMs })
    }

    @Test
    fun `explicit ms relative duration compiles`() {
        val timeline = compile(
            """
            [track lyrics cursor=main z=100 protect=on]
            [00:01.000]A
            [+500ms]B
            [endtrack]
            """.trimIndent(),
        )

        assertEquals(listOf(1000L, 1500L), timeline.events.map { it.timeMs })
    }

    @Test
    fun `track source relative time and final sorting are separate`() {
        val timeline = compile(
            """
            [track lyrics cursor=main z=100 protect=on]
            [00:10.000]A
            [00:05.000]B
            [+500]C
            [endtrack]
            """.trimIndent(),
        )

        assertEquals(listOf(5000L, 5500L, 10000L), timeline.events.map { it.timeMs })
        assertEquals(listOf("B", "C", "A"), timeline.events.map { (it.ops.single() as Text).value })
    }

    @Test
    fun `cue emit and loop are expanded at compile time`() {
        val timeline = compile(
            """
            [anchor intro 00:01.000 bpm=120]
            [cue blink cursor=fx z=20 protect=off]
            [loop 2]
            [+100]A
            [+200]B
            [endloop]
            [endcue]

            [track fx cursor=fx z=20 protect=off]
            [intro+1/2b][emit blink]
            [endtrack]
            """.trimIndent(),
        )

        assertEquals(listOf(1350L, 1550L, 1650L, 1850L), timeline.events.map { it.timeMs })
        assertEquals(listOf("A", "B", "A", "B"), timeline.events.map { (it.ops.single() as Text).value })
        assertTrue(timeline.events.all { it.source.startsWith("cue:blink") })
    }

    @Test
    fun `relative beat after absolute time fails without bpm context`() {
        val doc = KlipParser.parseText(
            """
            [track lyrics cursor=main z=100 protect=on]
            [00:01.000]A
            [+1b]B
            [endtrack]
            """.trimIndent(),
        )

        assertFailsWith<CompileError> {
            KlipCompiler().compile(doc)
        }
    }

    @Test
    fun `demo file compiles`() {
        val path = Path.of("examples/demo.klip")
        val doc = KlipParser.parse(path)
        val timeline = KlipCompiler().compile(doc)

        assertEquals(2, doc.anchors.size)
        assertEquals(2, doc.cues.size)
        assertEquals(2, doc.tracks.size)
        assertTrue(timeline.events.isNotEmpty())
        assertTrue(Files.exists(path))
    }

    private fun compile(text: String): Timeline =
        KlipCompiler().compile(KlipParser.parseText(text))
}
