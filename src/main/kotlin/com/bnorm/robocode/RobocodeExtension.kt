package com.bnorm.robocode

import com.bnorm.robocode.sf.SourceForge
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.Directory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.domainObjectContainer
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import java.io.File

open class RobocodeExtension(
    objects: ObjectFactory,
    layout: ProjectLayout,
    providerFactory: ProviderFactory
) {
    var download: Boolean by objects.property<Boolean>().apply {
        convention(true)
    }

    var downloadVersion: String by objects.property<String>().apply {
        convention(providerFactory.provider { SourceForge.findLatestVersion() })
    }

    internal var downloadDir: Directory by objects.directoryProperty().apply {
        convention(layout.buildDirectory.dir("robocode/download"))
    }

    var installDir: Directory by objects.directoryProperty().apply {
        val os = OperatingSystem.current()
        val robocodeHomeDir = if (os.isWindows) {
            providerFactory.provider { File("/") }
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        } else {
            providerFactory.systemProperty("user.home")
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        }
        convention(layout.dir(robocodeHomeDir))
    }

    val robocodeDir: Directory get() = if (download) downloadDir else installDir

    val robots = objects.domainObjectContainer(RobocodeRobot::class) { RobocodeRobot(it) }
    fun robots(action: Action<in NamedDomainObjectContainer<RobocodeRobot>>) {
        action.execute(robots)
    }
}
