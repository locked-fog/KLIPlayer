package kliplayer

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
