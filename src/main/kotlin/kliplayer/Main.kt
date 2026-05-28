package kliplayer

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val exitCode = Main().run(args)
    if (exitCode != 0) exitProcess(exitCode)
}

class Main {
    fun run(args: Array<String>): Int {
        if (args.size != 2 || args[0] !in setOf("play", "check", "compile")) {
            usage()
            return 2
        }
        return try {
            val path = Path.of(args[1])
            val document = KlipParser.parse(path)
            val timeline = KlipCompiler().compile(document)
            when (args[0]) {
                "check" -> check(timeline)
                "compile" -> compile(timeline)
                "play" -> play(timeline)
                else -> 2
            }
        } catch (error: KlipException) {
            System.err.println(error.message)
            1
        } catch (error: Exception) {
            System.err.println("KLP9001 runtime: ${error.message}")
            1
        }
    }

    private fun check(timeline: Timeline): Int {
        val doc = timeline.document
        println("file=${doc.fileName}")
        println("music=${doc.meta.music ?: "<none>"}")
        println("width=${doc.meta.width}")
        println("height=${doc.meta.height}")
        println("anchors=${doc.anchors.size}")
        println("cues=${doc.cues.size}")
        println("tracks=${doc.tracks.size}")
        println("events=${timeline.events.size}")
        println("range=${timeline.startMs}..${timeline.endMs}ms")
        if (doc.meta.music == null) {
            println("warning: music meta is missing; play will use monotonic no-audio mode")
        }
        return 0
    }

    private fun compile(timeline: Timeline): Int {
        for (event in timeline.events) {
            val ops = event.ops.joinToString(" | ") { it.describe() }
            println(
                "${event.timeMs}ms order=${event.order} z=${event.z} cursor=${event.cursorId} " +
                    "protect=${event.protect} line=${event.sourceLine} source=${event.source} :: $ops",
            )
        }
        return 0
    }

    private fun play(timeline: Timeline): Int {
        val renderer = TerminalRenderer(timeline.document.meta.width, timeline.document.meta.height)
        val audio = AudioPlayer.from(timeline.document, timeline.endMs)
        var index = 0
        try {
            audio.start()
            if (audio.modeMessage.startsWith("warning:")) {
                System.err.println(audio.modeMessage)
            }
            while (index < timeline.events.size || !audio.isFinished()) {
                val now = audio.currentMs()
                while (index < timeline.events.size && timeline.events[index].timeMs <= now) {
                    renderer.render(timeline.events[index])
                    index++
                }
                if (index >= timeline.events.size && audio.isFinished()) break
                Thread.sleep(5L)
            }
        } finally {
            audio.stop()
            renderer.restore()
        }
        return 0
    }

    private fun usage() {
        println("usage:")
        println("  kliplayer check <file.klip>")
        println("  kliplayer compile <file.klip>")
        println("  kliplayer play <file.klip>")
    }
}
