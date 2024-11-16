val exposedVersion: String by project
val h2Version: String by project

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.5"
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
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("dev.inmo:tgbotapi:18.2.2")
    implementation("dev.langchain4j:langchain4j:0.36.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.36.0")
    implementation("dev.langchain4j:langchain4j-chroma:0.36.0")
    implementation("dev.langchain4j:langchain4j-embeddings:0.36.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set("shadow")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    jar {
        archiveFileName.set("${project.name}-release.jar")

        manifest {
            attributes(
                "Main-Class" to "me.yailya.step_ahead_bot.MainKt"
            )
        }
    }
}