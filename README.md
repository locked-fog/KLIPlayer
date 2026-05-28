# KLIPlayer

KLIPlayer is a Kotlin/JVM rewrite of CLIPlayer. It reads `.klip` scripts, expands track/cue/emit/loop blocks into a compile-time timeline, and renders ANSI terminal performances with Z-axis protection.

This project is intentionally small for the initial core:

- Kotlin/JVM, target JVM 21.
- Single Gradle module.
- Runnable JAR output.
- ANSI terminal renderer.
- Compile-time timeline expansion.
- `ProtectionMask` without a full virtual screen.
- CJK-aware display width.

## Build

```sh
./gradlew test
./gradlew build
```

The runnable JAR is produced under `build/libs/`.

## CLI

```sh
./gradlew run --args="check examples/demo.klip"
./gradlew run --args="compile examples/demo.klip"
./gradlew run --args="play examples/demo.klip"
```

Commands:

- `check <file.klip>` parses and compiles a script, then prints summary information.
- `compile <file.klip>` prints the expanded event table.
- `play <file.klip>` plays the expanded timeline through the ANSI renderer.

Audio playback is encapsulated behind `AudioClock`. The initial implementation tries Java Sound for locally supported formats and falls back to a monotonic clock when audio is missing or unsupported.
