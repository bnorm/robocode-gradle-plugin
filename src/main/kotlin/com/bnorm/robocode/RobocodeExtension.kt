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
        // Download Robocode by default to make it easier for CI/CD environments
        convention(true)
    }

    var downloadVersion: String by objects.property<String>().apply {
        // Download the latests version of Robocode by default
        convention(providerFactory.provider { SourceForge.findLatestVersion() })
    }

    internal var downloadDir: Directory by objects.directoryProperty().apply {
        // Default directory is outside build directory to avoid a clean clearing robot cache
        convention(layout.projectDirectory.dir(".robocode"))
    }

    var installDir: Directory by objects.directoryProperty().apply {
        val os = OperatingSystem.current()
        val robocodeHomeDir = if (os.isWindows) {
            providerFactory.provider { File("/") /* C:\ directory */ }
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        } else {
            providerFactory.systemProperty("user.home")
                .forUseAtConfigurationTime()
                .map { File(it, "robocode") }
        }
        // Default to the default install directory of Robocode
        convention(layout.dir(robocodeHomeDir))
    }

    val robocodeDir: Directory get() = if (download) downloadDir else installDir

    val robots = objects.domainObjectContainer(RobocodeRobot::class) { RobocodeRobot(it) }
    fun robots(action: Action<in NamedDomainObjectContainer<RobocodeRobot>>) {
        action.execute(robots)
    }
}
