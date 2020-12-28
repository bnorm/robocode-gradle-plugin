package com.bnorm.robocode

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.the

class RobocodePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply<JavaLibraryPlugin>()
            apply<ShadowPlugin>()

            val robocode = extensions.create<RobocodeExtension>("robocode")

            val robocodeBuildDir = "$buildDir/robocode"
            val generatedBuildDir = "$robocodeBuildDir/generated"

            val implementation by configurations.getting
            dependencies {
                implementation(files(robocode.robocodeDir.dir("libs").file("robocode.jar")))
            }

            val main = the<SourceSetContainer>().named("main") {
                resources.srcDirs(file(generatedBuildDir))
            }

            val robocodeDownload by tasks.registering(RobocodeDownload::class) {
                this.enabled = robocode.download
                this.downloadDir = robocode.downloadDir.toString()
                this.downloadVersion = robocode.downloadVersion
            }

            implementation.buildDependencies

            tasks.named("compileJava").configure { dependsOn(robocodeDownload) }
            plugins.withId("org.jetbrains.kotlin.jvm") {
                tasks.named("compileKotlin").configure { dependsOn(robocodeDownload) }
            }

            val robocodeRun by tasks.registering(JavaExec::class) {
                dependsOn(robocodeDownload)

                workingDir(robocode.robocodeDir)
                classpath(main.get().runtimeClasspath)
                mainClass.set("robocode.Robocode")
            }

            val createVersion by tasks.registering {
                for (robot in robocode.robots) {
                    val properties = mapOf(
                        "robocode.version" to "1.9",
                        "robot.name" to robot.name,
                        "robot.classname" to robot.classPath,
                        "robot.version" to robot.version
                    )

                    doLast {
                        val propertiesFile = file("$generatedBuildDir/${robot.name}.properties")
                        propertiesFile.parentFile.mkdirs()
                        propertiesFile.writeText(properties.entries
                            .filter { (_, v) -> v != null }
                            .joinToString(separator = "\n") { (k, v) -> "$k=$v" })
                    }
                }
            }

            tasks.named("processResources") {
                dependsOn(createVersion)
            }

            val shadowJar by tasks.named<ShadowJar>("shadowJar") {
                exclude("/META-INF/**")
                exclude("/gl4java/**")
                exclude("/net/sf/robocode/**")
                exclude("/robocode/**")
            }

            tasks.register<Sync>("unpack") {
                dependsOn(shadowJar)
                from(zipTree(shadowJar.archiveFile))
                into("$robocodeBuildDir/bin")
            }
        }
    }
}
