# robocode-gradle-plugin

> Build the best...

[Gradle] plugin for building (and running) bots for [Robocode]. To use, add the
[latest version of the plugin][com.bnorm.robocode] to a project.

```kotlin
plugins {
    id("com.bnorm.robocode") version "0.1.1"
}
```

Robocode doesn't natively allow robots to depend on third-party jars, but this
plugin uses the [Shadow] Gradle plugin to combine everything into a single jar
to work around this problem.

# Robots

Robots can be configured via the plugin extension. Multiple robots can be
configured if desired.

```kotlin
robocode {
    robots {
        register("Name") {
            classPath = "package.Name"
            version = "1.0"
            description = "Description"
        }
    }
}
```

For each added robot, a task is added for building a jar file which can be
published for others to use. For example, given a robot with the name of
`Name`, a task named `robotNameJar` will be available which outputs to 
`$buildDir/robocode/robots/Name/Name_1.0.jar`.

For robot development, a task named `robotBin` is available which outputs all
class files needed to run the robot via Robocode. This task can be run with
Gradle continuous mode to constantly build all robots.

```shell
$ ./gradlew robotBin --continuous
```

# Robocode

A dependency on the `robocode.jar` file is automatically added to the
`implementation` configuration by the plugin. To achieve this, the file must
already exist in an existing installed version of Robocode or be downloaded by
the plugin. If this behavior is not the desired, use the following configuration
options available in the plugin extension.

```kotlin
robocode {
    download = true // default - if Robocode should be downloaded, otherwise the installDir is used
    downloadVersion = <default version> // default - version of Robocode to download
    downloadDir = layout.projectDirectory.dir(".robocode") // default - where Robocode should be downloaded
    installDir = <default install directory> // default - the location where Robocode is installed
}
```

When Robocode is downloaded, it will automatically add the development output
folder to the Robocode configuration. This allows Robocode to automatically use
the development build of robots.

Also, because the plugin knows the location of a Robocode installation, either
specified or downloaded, it means that Robocode can be run directly from Gradle.
To do so, use the task `robocodeRun`.

# Test Battles

Much of robot development is running battles against other robots for testing.
Because of this, the plugin makes all Robocode jar files available in a Gradle
configuration called `robocodeRuntime`. Gradle can be configured to add a source
set which extends from this configuration for running battles.

```kotlin
val battles by sourceSets.registering

val battlesImplementation by configurations.getting {
    extendsFrom(configurations.robocode.get())
}

val battlesRuntimeOnly by configurations.getting {
    extendsFrom(configurations.robocodeRuntime.get())
}

dependencies {
    // Use JUnit to run the battles as "tests" 
    battlesImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    battlesRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

val runBattles by project.tasks.registering(Test::class) {
    dependsOn("robotBin")

    description = "Runs Robocode battles"
    group = "battles"

    useJUnitPlatform()
    testClassesDirs = battles.get().output.classesDirs
    classpath = battles.get().runtimeClasspath
}
```

[Gradle]: https://gradle.org/
[Robocode]: https://robocode.sourceforge.io/
[com.bnorm.robocode]: https://plugins.gradle.org/plugin/com.bnorm.robocode
[Shadow]: https://imperceptiblethoughts.com/shadow/
