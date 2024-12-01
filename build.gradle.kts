repositories {
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.gradleup.shadow")
    }

    repositories {
        mavenCentral()
    }

    kotlin {
        jvmToolchain(21)
    }
}