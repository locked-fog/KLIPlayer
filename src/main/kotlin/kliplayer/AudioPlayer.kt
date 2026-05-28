package kliplayer

import java.nio.file.Files
import java.nio.file.Path
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

interface AudioClock {
    fun start()
    fun currentMs(): Long
    fun stop()
    fun isFinished(): Boolean
}

class AudioPlayer private constructor(
    private val musicPath: Path?,
    private val fallbackDurationMs: Long,
) : AudioClock {
    private var clip: Clip? = null
    private var startNanos: Long = 0L
    private var started = false

    override fun start() {
        started = true
        startNanos = System.nanoTime()
        val path = musicPath
        if (path != null && Files.exists(path)) {
            runCatching {
                AudioSystem.getAudioInputStream(path.toFile()).use { stream ->
                    val loaded = AudioSystem.getClip()
                    loaded.open(stream)
                    loaded.start()
                    clip = loaded
                }
            }.onFailure {
                clip = null
                startNanos = System.nanoTime()
            }
        }
    }

    override fun currentMs(): Long {
        val loaded = clip
        if (loaded != null) return loaded.microsecondPosition / 1_000L
        if (!started) return 0L
        return (System.nanoTime() - startNanos) / 1_000_000L
    }

    override fun stop() {
        clip?.stop()
        clip?.close()
        clip = null
    }

    override fun isFinished(): Boolean {
        val loaded = clip
        if (loaded != null) {
            return loaded.microsecondLength > 0 && loaded.microsecondPosition >= loaded.microsecondLength
        }
        return currentMs() >= fallbackDurationMs
    }

    companion object {
        fun from(document: KlipDocument, timelineEndMs: Long): AudioPlayer {
            val music = document.meta.music?.let {
                val path = Path.of(it)
                if (path.isAbsolute) path else Path.of(document.fileName).parent?.resolve(path) ?: path
            }
            return AudioPlayer(music, timelineEndMs + 250L)
        }
    }
}
