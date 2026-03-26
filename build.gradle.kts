plugins {
    kotlin("jvm") version "2.3.10"
    application
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.lockedfog"
version = "0.0.1-snapshot"

repositories {
    mavenCentral()
}

dependencies {
    //coroutines, serialization and kotlin scripts
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.10")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.lockedfog.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.lockedfog.MainKt"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.lockedfog.MainKt"
    }
}

kotlin {
    jvmToolchain(21)
}


tasks.test {
    useJUnitPlatform()
}