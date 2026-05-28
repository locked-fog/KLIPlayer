package kliplayer

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AudioPlayerTest {
    @Test
    fun `missing music meta uses explicit no-audio status`() {
        val document = KlipDocument(
            fileName = "example.klip",
            meta = Meta(emptyMap()),
            anchors = emptyList(),
            cues = emptyList(),
            tracks = emptyList(),
        )
        val player = AudioPlayer.from(document, timelineEndMs = 0L)

        player.start()

        assertIs<AudioStatus.NoMusicConfigured>(player.status)
        assertTrue(player.status.isFallback)
        assertContains(player.status.message, "music meta is missing")
        player.stop()
    }

    @Test
    fun `missing audio file resolves relative to script directory`() {
        val scriptDir = Files.createTempDirectory("kliplayer-audio")
        val document = KlipDocument(
            fileName = scriptDir.resolve("show.klip").toString(),
            meta = Meta(mapOf("music" to "missing.mp3")),
            anchors = emptyList(),
            cues = emptyList(),
            tracks = emptyList(),
        )
        val player = AudioPlayer.from(document, timelineEndMs = 0L)

        player.start()

        val status = assertIs<AudioStatus.MissingFile>(player.status)
        assertTrue(status.isFallback)
        assertTrue(status.path.endsWith("missing.mp3"))
        assertTrue(status.path.startsWith(scriptDir))
        player.stop()
        Files.deleteIfExists(scriptDir)
    }

    @Test
    fun `unsupported audio file reports failed fallback status`() {
        val audioFile = Files.createTempFile("kliplayer-invalid-audio", ".mp3")
        Files.writeString(audioFile, "not an audio file")
        val document = KlipDocument(
            fileName = audioFile.resolveSibling("show.klip").toString(),
            meta = Meta(mapOf("music" to audioFile.fileName.toString())),
            anchors = emptyList(),
            cues = emptyList(),
            tracks = emptyList(),
        )
        val player = AudioPlayer.from(document, timelineEndMs = 0L)

        player.start()

        assertIs<AudioStatus.Failed>(player.status)
        assertTrue(player.status.isFallback)
        assertContains(player.status.message, "audio could not be started")
        player.stop()
        Files.deleteIfExists(audioFile)
    }
}
