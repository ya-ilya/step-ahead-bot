val exposedVersion: String by project
val h2Version: String by project
val langchainVersion: String by project
val serializationVersion: String by project
val tgbotapiVersion: String by project
val mysqlVersion: String by project

group = "me.yailya"
version = "0.1"

plugins {
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    implementation("dev.langchain4j:langchain4j:$langchainVersion")
    implementation("dev.langchain4j:langchain4j-ollama:$langchainVersion")
    implementation("dev.langchain4j:langchain4j-embeddings:$langchainVersion")
    implementation("dev.inmo:tgbotapi:$tgbotapiVersion")

    implementation("com.mysql:mysql-connector-j:$mysqlVersion")
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