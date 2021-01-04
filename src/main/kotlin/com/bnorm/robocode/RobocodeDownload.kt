package com.bnorm.robocode

import com.bnorm.robocode.sf.SourceForge
import okio.sink
import okio.buffer
import okio.source
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

    @get:Input
    var downloadVersion by project.objects.property<String>()

    /*
     * The only output for this task to care about is the libs directory. Everything else *should*
     * be ignored so this task remains UP-TO-DATE even after running Robocode.
     */
    // TODO Should we only care about specific files in the libs directory? Only Jar files?
    @get:OutputDirectory
    val libsDir: Provider<Directory>
        get() = project.layout.dir(project.provider { project.file("$downloadDir/libs") })

    @TaskAction
    fun perform() {
        val setupJar = project.file("$downloadDir/robocode-$downloadVersion-setup.jar")

        setupJar.parentFile.mkdirs()
        SourceForge.download(downloadVersion).use { source ->
            source.readAll(setupJar.sink())
        }

        /*
         * Use copy and *not* sync to avoid deleting bot jar files which have been downloaded and
         * any configuration that has been changed by running Robocode.
         */
        project.copy {
            from(project.zipTree(setupJar))
            into(downloadDir)
        }

        /*
         * Automatically add the bot 'bin' directory to the development path of Robocode. This
         * avoids needing to manually configure the directory the first time Robocode is installed.
         */
        val propertiesFile = project.file("$downloadDir/config/robocode.properties")
        propertiesFile.parentFile.mkdirs()

        val properties = if (!propertiesFile.exists()) emptyList()
        else propertiesFile.source().buffer().readUtf8().split("\n")

        val devPath = "${project.buildDir}/robocode/robots/bin"
        propertiesFile.sink().buffer().use { sink ->
            var foundDevPath = false
            for (property in properties) {
                sink.writeUtf8(property)
                if ("robocode.options.development.path=" in property) {
                    foundDevPath = true
                    if (devPath !in property) {
                        sink.writeUtf8(",").writeUtf8(devPath)
                    }
                }
                sink.writeUtf8("\n")
            }
            if (!foundDevPath) {
                sink.writeUtf8("robocode.options.development.path=$devPath\n")
            }
        }
    }
}
