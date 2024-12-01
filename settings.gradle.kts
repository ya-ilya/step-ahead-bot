rootProject.name = "step-ahead-bot"

pluginManagement {
    val kotlinVersion: String by settings
    val shadowVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("com.gradleup.shadow") version shadowVersion
    }
}

include(
    "bot"
)