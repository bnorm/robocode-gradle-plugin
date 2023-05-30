plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "com.bnorm.robocode"
version = "0.3.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Plugins
    implementation("com.github.johnrengelman:shadow:8.1.1")

    // Libraries
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
}

gradlePlugin {
    website.set("https://github.com/bnorm/robocode-gradle-plugin")
    vcsUrl.set("https://github.com/bnorm/robocode-gradle-plugin.git")
    plugins {
        register("robocodePlugin") {
            id = "com.bnorm.robocode"
            displayName = "Robocode Gradle Plugin"
            description = "Gradle plugin for developing Robocode bots"
            implementationClass = "com.bnorm.robocode.RobocodePlugin"
            tags.set(listOf("robocode"))
        }
    }
}

kotlin {
    jvmToolchain(8)
}
