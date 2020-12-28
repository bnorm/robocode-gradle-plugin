plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "0.12.0"
}

group = "com.bnorm.robocode"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Plugins
    implementation("com.github.jengelman.gradle.plugins:shadow:6.1.0")

    // Libraries
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.12.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.0")
}

gradlePlugin {
    plugins {
        register("robocodePlugin") {
            id = "com.bnorm.robocode"
            implementationClass = "com.bnorm.robocode.RobocodePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/bnorm/robocode-gralde-plugin"
    vcsUrl = "https://github.com/bnorm/robocode-gralde-plugin.git"
    tags = listOf("robocode")
}
