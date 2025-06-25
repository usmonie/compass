/**
 * Compass Libraries Version Configuration
 *
 * This script loads version information from compass-versions.properties
 * and makes it available to all Compass library build scripts.
 */

// Load compass versions from properties file
val compassVersionsFile = rootProject.file("gradle/compass-versions.properties")
val compassVersionsProps = java.util.Properties().apply {
    if (compassVersionsFile.exists()) {
        load(compassVersionsFile.inputStream())
    }
}

// Extension to access compass versions
val Project.compassVersions: CompassVersions
    get() = CompassVersions(compassVersionsProps)

data class CompassVersions(private val properties: java.util.Properties) {
    val buildVersion: String
        get() = properties.getProperty("compass.build.version") ?: "0.2.1"

    val coreVersion: String
        get() = properties.getProperty("compass.core.version") ?: buildVersion

    val stateVersion: String
        get() = properties.getProperty("compass.state.version") ?: buildVersion

    val componentStateVersion: String
        get() = properties.getProperty("compass.component.state.version") ?: buildVersion

    val screenStateVersion: String
        get() = properties.getProperty("compass.screen.state.version") ?: buildVersion

    val groupId: String
        get() = properties.getProperty("compass.group.id") ?: "com.usmonie.compass"
}

// Make versions available to all compass library projects
if (project.path.startsWith(":compass:")) {
    // Set version for compass libraries
    project.version = project.compassVersions.buildVersion

    // Set specific group ID based on the library
    project.group = when (project.name) {
        "core" -> "${project.compassVersions.groupId}.core"
        "state" -> "${project.compassVersions.groupId}.state"
        "component-state" -> "${project.compassVersions.groupId}.component.state"
        "screen-state" -> "${project.compassVersions.groupId}.screen.state"
        else -> project.compassVersions.groupId
    }
}