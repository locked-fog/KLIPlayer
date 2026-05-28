package kliplayer

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
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
        val error = assertCompileFails(
            "KLP5001",
            """
            [track lyrics cursor=main z=100 protect=on]
            [00:01.000]A
            [+1b]B
            [endtrack]
            """.trimIndent(),
            "相对节拍缺少 BPM 上下文",
        )
        assertEquals(3, error.sourceLine)
    }

    @Test
    fun `reports duplicate anchors and cues`() {
        assertCompileFails(
            "KLP3002",
            """
            [anchor intro 00:00.000 bpm=120]
            [anchor intro 00:01.000 bpm=120]
            """.trimIndent(),
            "重复定义 anchor",
        )
        assertCompileFails(
            "KLP4002",
            """
            [cue flash cursor=fx z=1 protect=off]
            [+0]A
            [endcue]
            [cue flash cursor=fx z=1 protect=off]
            [+0]B
            [endcue]
            """.trimIndent(),
            "重复定义 cue",
        )
    }

    @Test
    fun `reports undefined anchor and cue`() {
        assertCompileFails(
            "KLP3001",
            """
            [track lyrics cursor=main z=100 protect=on]
            [missing+1b]A
            [endtrack]
            """.trimIndent(),
            "未定义 anchor",
        )
        assertCompileFails(
            "KLP4001",
            """
            [track fx cursor=fx z=1 protect=off]
            [00:00.000][emit nope]
            [endtrack]
            """.trimIndent(),
            "未定义 cue",
        )
    }

    @Test
    fun `reports illegal cue time and malformed durations`() {
        assertCompileFails(
            "KLP2001",
            """
            [cue bad cursor=fx z=1 protect=off]
            [00:01.000]A
            [endcue]
            [track fx cursor=fx z=1 protect=off]
            [00:00.000][emit bad]
            [endtrack]
            """.trimIndent(),
            "cue 内只允许使用相对时间",
        )
        assertCompileFails(
            "KLP5001",
            """
            [track lyrics cursor=main z=100 protect=on]
            [00:60.000]A
            [endtrack]
            """.trimIndent(),
            "绝对时间无法解析",
        )
        assertCompileFails(
            "KLP5001",
            """
            [anchor intro 00:00.000 bpm=120]
            [track lyrics cursor=main z=100 protect=on]
            [intro++2b]A
            [endtrack]
            """.trimIndent(),
            "节拍 duration 无法解析",
        )
        assertCompileFails(
            "KLP5001",
            """
            [anchor intro 00:00.000 bpm=120]
            [track lyrics cursor=main z=100 protect=on]
            [intro+1/0b]A
            [endtrack]
            """.trimIndent(),
            "分数节拍分母不能为 0",
        )
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

    private fun assertCompileFails(code: String, script: String, expectedMessage: String): CompileError {
        val error = assertFailsWith<CompileError> {
            compile(script)
        }
        assertEquals(code, error.code)
        assertContains(error.message ?: "", expectedMessage)
        return error
    }
}
