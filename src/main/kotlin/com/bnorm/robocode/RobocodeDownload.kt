package com.bnorm.robocode

import com.bnorm.robocode.sf.SourceForge
import okio.sink
import okio.buffer
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue

open class RobocodeDownload : DefaultTask() {
    @get:Input
    var downloadDir by project.objects.property<String>()

    // Only care about the libs directory contents changing
    // This allows the robocode install to actually be used
    @get:OutputDirectory
    val libsDir: Provider<Directory>
        get() = project.layout.dir(project.provider { project.file("$downloadDir/libs") })

    @get:Input
    var downloadVersion by project.objects.property<String>()

    @TaskAction
    fun perform() {
        val setupJar = project.file("$downloadDir/robocode-$downloadVersion-setup.jar")

        setupJar.parentFile.mkdirs()
        SourceForge.download(downloadVersion).use { source ->
            source.readAll(setupJar.sink())
        }

        project.sync {
            from(project.zipTree(setupJar))
            into(downloadDir)
        }

        val properties = project.file("$downloadDir/config/robocode.properties")
        properties.parentFile.mkdirs()
        properties.sink().buffer().use {sink ->
            sink.writeUtf8("robocode.options.development.path=${project.buildDir}/robocode/bin")
        }
    }
}
