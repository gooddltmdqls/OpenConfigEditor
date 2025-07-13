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
    compileOnly("xyz.icetang.lib:invfx-api:${properties["invfxVersion"]!!}")
}

tasks.withType<ProcessResources> {
    inputs.property("version", version)
    inputs.property("pluginName", pluginName)
    inputs.property("bukkitApiVersion", properties["bukkitApiVersion"]!!)
    inputs.property("icemmandVersion", properties["icemmandVersion"]!!)
    inputs.property("invfxVersion", properties["invfxVersion"]!!)

    filesMatching("plugin.yml") {
        expand(inputs.properties)
    }
}

tasks.withType<RunServer> {
    minecraftVersion("1.21.7")

    minHeapSize = "8192M"
    maxHeapSize = "8192M"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}