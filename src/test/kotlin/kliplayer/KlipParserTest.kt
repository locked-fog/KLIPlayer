package kliplayer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KlipParserTest {
    @Test
    fun `parses meta anchor cue and track`() {
        val doc = KlipParser.parseText(
            """
            [meta music="song.mp3"]
            [meta width=80]
            [meta height=24]
            [anchor intro 00:01.000 bpm=120]

            [cue flash cursor=warn z=80 protect=off]
            [+0][mv 1,2]GO
            [endcue]

            [track lyrics cursor=main z=100 protect=on]
            [intro+1b][mv 3,4]熱異常
            [endtrack]
            """.trimIndent(),
        )

        assertEquals("song.mp3", doc.meta.music)
        assertEquals(80, doc.meta.width)
        assertEquals(24, doc.meta.height)
        assertEquals(1, doc.anchors.size)
        assertEquals(1, doc.cues.size)
        assertEquals(1, doc.tracks.size)
        assertIs<Move>(doc.tracks.single().entries.single().ops.first())
    }

    @Test
    fun `keeps url text containing slash slash`() {
        val doc = KlipParser.parseText(
            """
            [track links cursor=main z=1 protect=off]
            [00:01.000]https://github.com/locked-fog/CLIPlayer
            [endtrack]
            """.trimIndent(),
        )

        val text = doc.tracks.single().entries.single().ops.single() as Text
        assertEquals("https://github.com/locked-fog/CLIPlayer", text.value)
    }

    @Test
    fun `comments whitespace and escapes are parsed by spec rules`() {
        val doc = KlipParser.parseText(
            """
            // full line comment
              // indented full line comment
            [track text cursor=main z=1 protect=off]
              [00:00.000]   [mv 1,1]   escaped \[tag\] \\ \n done
            [endtrack]
            """.trimIndent(),
        )

        val ops = doc.tracks.single().entries.single().ops
        assertIs<Move>(ops[0])
        assertEquals("   escaped [tag] \\ \n done", (ops[1] as Text).value)
    }

    @Test
    fun `parses background style newline hide show clean commands`() {
        val doc = KlipParser.parseText(
            """
            [track commands cursor=main z=1 protect=off]
            [00:00.000][background 101010][style bold on][newline][hide][show][cleanline][clear]
            [endtrack]
            """.trimIndent(),
        )

        val ops = doc.tracks.single().entries.single().ops
        assertIs<Background>(ops[0])
        assertIs<Style>(ops[1])
        assertEquals(Newline, ops[2])
        assertEquals(HideCursor, ops[3])
        assertEquals(ShowCursor, ops[4])
        assertEquals(CleanLine, ops[5])
        assertEquals(Clear, ops[6])
    }

    @Test
    fun `cue rejects wrong endtrack terminator during parse`() {
        assertFailsWith<ParseError> {
            KlipParser.parseText(
                """
                [cue bad cursor=fx z=1 protect=off]
                [endtrack]
                """.trimIndent(),
            )
        }
    }
}
