val exposedVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.20-1.0.25"
    id("eu.vendeli.telegram-bot") version "7.3.1"
}

group = "me.yailya"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

kotlin {
    jvmToolchain(21)
}