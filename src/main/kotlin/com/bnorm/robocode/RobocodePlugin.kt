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

            val extension = extensions.create<RobocodeExtension>("robocode")
            val robocodeBuildDir = project.layout.buildDirectory.dir("robocode")

            /*
             * Create 2 configurations `robocode` and `robocodeRuntime`. The robocode configuration
             * will depend only on the robocode.jar file. This allows bots to be built and for a
             * task which can run Robocode from the install directory. The robocodeRuntime
             * configuration is available to extend from if needed for running battles from a unit
             * or integration test.
             */
            val robocode by configurations.registering
            val robocodeRuntime by configurations.registering
            dependencies {
                // Add only the robocode jar for building bots
                robocode.get().invoke(files(extension.robocodeDir.dir("libs").file("robocode.jar")))
                // Add the entire libs directory for running battles
                robocodeRuntime.get().invoke(extension.robocodeDir.dir("libs").asFileTree)
            }
            configurations.named("implementation") { extendsFrom(robocode.get()) }

            /*
             * Task which downloads and unpacks the specified version (latest by default) of
             * Robocode to a project level directory. This bootstraps the project which everything
             * required to build a bot in a CI/CD environment.
             */
            val robocodeDownload by tasks.registering(RobocodeDownload::class) {
                group = "robocode"

                enabled = extension.download
                downloadDir = extension.downloadDir.toString()
                downloadVersion = extension.downloadVersion
            }

            /*
             * Make compilation depend on downloading Robocode so the robocode.jar dependency is
             * available.
             */
            // TODO Is there a better way to do this? Make configuration resolution dependent on the task?
            tasks.named("compileJava").configure { dependsOn(robocodeDownload) }
            plugins.withId("org.jetbrains.kotlin.jvm") {
                tasks.named("compileKotlin").configure { dependsOn(robocodeDownload) }
            }

            /*
             * Task for running Robocode with Gradle. This will either run the downloaded version
             * or the locally installed version depending on what is configured.
             */
            tasks.register<JavaExec>("robocodeRun") {
                group = "robocode"

                dependsOn(robocodeDownload)

                workingDir(extension.robocodeDir)
                classpath(robocode.get().files)
                mainClass.set("robocode.Robocode")
            }

            /*
             * ShadowJar for bundling compiled and dependency class files into a single jar file.
             * Robocode requires a single jar file contain no other jar files to run a bot. This
             * works around the natural inability of Robocode to allow dependencies.
             */
            val shadowJar by tasks.named<ShadowJar>("shadowJar") {
                excludeRobocode() // Exclude robocode.jar dependency
            }

            /*
             * Extract the ShadowJar into a 'bin' directory to be loaded into Robocode for active
             * development. Robocode allows a local build directory to be indexed for bots, this
             * makes sure all dependency class files are present in the output.
             */
            tasks.register<Sync>("robotBin") {
                group = "robocode"

                dependsOn(shadowJar)
                from(zipTree(shadowJar.archiveFile))
                into(robocodeBuildDir.map { it.dir("robots/bin") })
            }

            afterEvaluate {
                /*
                 * Plugin allows multiple bots to be packaged from the same source set. Go through
                 * each bot specified and create the required tasks.
                 */
                for (robot in extension.robots) {
                    val robotBuildDir = robocodeBuildDir.map { it.dir("robots/${robot.name}") }
                    val robotResDir = robotBuildDir.map { it.dir("res") }

                    /*
                     * Task for creating the properties files required for each bot. This properties
                     * file is only required when publishing the bot, and isn't required for local
                     * development.
                     */
                    val createVersion by tasks.register("robot${robot.name}Properties") {
                        group = "robocode"

                        val propertiesFileName = "${robot.classPath.replace('.', '/')}.properties"
                        val propertiesFile = robotResDir.map { it.file(propertiesFileName) }.get().asFile
                        val properties = mapOf(
                            "robocode.version" to "1.9",
                            "robot.name" to robot.name,
                            "robot.classname" to robot.classPath,
                            "robot.version" to robot.version,
                            "robot.description" to robot.description
                        ).filterValues { it != null }

                        inputs.properties(properties)
                        outputs.file(propertiesFile)

                        doLast {
                            propertiesFile.parentFile.mkdirs()
                            propertiesFile.writeText(properties.entries
                                .joinToString(separator = "\n") { (k, v) -> "$k=$v" })
                        }
                    }

                    /*
                     * Build a publishable jar file for the bot. The jar file will contain the
                     * generated properties file and all source code. Also use ShadowJar to bundle
                     * all class files correct, including those from dependencies.
                     */
                    tasks.register<ShadowJar>("robot${robot.name}Jar") {
                        group = "robocode"

                        dependsOn(createVersion)

                        excludeRobocode() // Exclude robocode.jar dependency

                        // Configure jar file name and output directory
                        archiveFileName.set("${robot.classPath}_${robot.version}.jar")
                        destinationDirectory.set(robotBuildDir)

                        // Configure source code, properties file, and all class files
                        val main by the<SourceSetContainer>().named("main")
                        from(main.output)
                        from(main.allSource)
                        from(robotResDir)
                        configurations = listOf(project.configurations.named("runtimeClasspath").get())
                    }
                }
            }
        }
    }

    /**
     * Exclude files which come from the robocode.jar file and other files which Robocode does not
     * like to appear in a jar file when importing.
     */
    private fun ShadowJar.excludeRobocode() {
        exclude("/gl4java/**")
        exclude("/net/sf/robocode/**")
        exclude("/robocode/**")

        // Filter out other misc files Robocode doesn't like
        exclude("/META-INF/**/*.properties")
        exclude("/META-INF/**/*.xml")
        exclude("/META-INF/**/*.class")
        exclude("/META-INF/**/*.kotlin_module")
    }
}
