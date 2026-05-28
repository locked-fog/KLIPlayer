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

sealed interface AudioStatus {
    val message: String
    val isFallback: Boolean

    data object NotStarted : AudioStatus {
        override val message: String = "audio: not started"
        override val isFallback: Boolean = false
    }

    data class Playing(val path: Path) : AudioStatus {
        override val message: String = "audio: playing $path"
        override val isFallback: Boolean = false
    }

    data object NoMusicConfigured : AudioStatus {
        override val message: String = "warning: music meta is missing; using monotonic no-audio clock"
        override val isFallback: Boolean = true
    }

    data class MissingFile(val path: Path) : AudioStatus {
        override val message: String = "warning: audio file not found: $path; using monotonic no-audio clock"
        override val isFallback: Boolean = true
    }

    data class Failed(val path: Path, val reason: String) : AudioStatus {
        override val message: String = "warning: audio could not be started for $path ($reason); using monotonic no-audio clock"
        override val isFallback: Boolean = true
    }

    data object Stopped : AudioStatus {
        override val message: String = "audio: stopped"
        override val isFallback: Boolean = false
    }
}

class AudioPlayer private constructor(
    private val musicPath: Path?,
    private val fallbackDurationMs: Long,
) : AudioClock {
    private var clip: Clip? = null
    private var startNanos: Long = 0L
    private var started = false
    var status: AudioStatus = AudioStatus.NotStarted
        private set

    val modeMessage: String get() = status.message

    override fun start() {
        started = true
        startNanos = System.nanoTime()
        val path = musicPath
        if (path == null) {
            status = AudioStatus.NoMusicConfigured
            return
        }
        if (!Files.exists(path)) {
            status = AudioStatus.MissingFile(path)
            return
        }
        runCatching {
            AudioSystem.getAudioInputStream(path.toFile()).use { stream ->
                val loaded = AudioSystem.getClip()
                loaded.open(stream)
                loaded.start()
                clip = loaded
                status = AudioStatus.Playing(path)
            }
        }.onFailure {
            clip = null
            startNanos = System.nanoTime()
            status = AudioStatus.Failed(path, it.message ?: it::class.simpleName ?: "unknown error")
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
        status = AudioStatus.Stopped
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
