plugins {
    kotlin("jvm") version "2.3.10"
    application
    id("com.gradleup.shadow") version "9.3.0"
}

group = "com.lockedfog.kliplayer"
version = "0.0.1-snapshot"

repositories {
    mavenCentral()
}

dependencies {
    //coroutines and kotlin scripts
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:2.3.20")


    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.lockedfog.kliplayer.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.lockedfog.kliplayer.MainKt"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "com.lockedfog.kliplayer.MainKt"
    }
}

tasks.distZip {
    dependsOn(tasks.shadowJar)
}

tasks.distTar {
    dependsOn(tasks.shadowJar)
}

tasks.startScripts {
    dependsOn(tasks.shadowJar)
}

tasks.startShadowScripts {
    dependsOn(tasks.jar)
}

kotlin {
    jvmToolchain(21)
}


tasks.test {
    useJUnitPlatform()
}