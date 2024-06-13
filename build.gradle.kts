import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    kotlin("jvm") version "1.9.22"
    id("xyz.jpenilla.run-paper") version "2.2.2"
    application
}

group = "xyz.icetang.plugin"
version = properties["version"]!!

val pluginName = properties["pluginName"]!!

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${properties["paperApiVersion"]!!}-R0.1-SNAPSHOT")
    compileOnly("xyz.icetang.lib:icemmand-api:${properties["icemmandVersion"]!!}")
    compileOnly("io.github.monun:invfx-api:3.3.2")
}

tasks.withType<ProcessResources> {
    inputs.property("version", version)
    inputs.property("pluginName", pluginName)
    inputs.property("bukkitApiVersion", properties["bukkitApiVersion"]!!)
    inputs.property("icemmandVersion", properties["icemmandVersion"]!!)

    filesMatching("plugin.yml") {
        expand(mapOf(
            Pair("version", version),
            Pair("pluginName", pluginName),
            Pair("bukkitApiVersion", properties["bukkitApiVersion"]!!),
            Pair("icemmandVersion", properties["icemmandVersion"]!!)
        ))
    }
}

tasks.withType<RunServer> {
    minecraftVersion(properties["paperApiVersion"]!! as String)

    minHeapSize = "1536M"
    maxHeapSize = "1536M"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}