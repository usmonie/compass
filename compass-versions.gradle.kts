/**
 * Compass Libraries Version Configuration
 *
 * This script loads version information from compass-versions.properties
 * and makes it available to all Compass library build scripts.
 */

// Load compass versions from properties file
val compassVersionsFile = listOf(
    rootProject.file("gradle/compass-versions.properties"),
    rootProject.file("compass-versions.properties"),
    project.file("compass-versions.properties")
).firstOrNull { it.exists() }

val compassVersionsProps = java.util.Properties().apply {
    compassVersionsFile?.inputStream()?.use { load(it) }
}

// Extension to access compass versions
val Project.compassVersions: CompassVersions
    get() = CompassVersions(this, compassVersionsProps)

data class CompassVersions(private val project: Project, private val properties: java.util.Properties) {
    private fun getProperty(key: String, default: String): String {
        return project.findProperty(key) as? String
            ?: properties.getProperty(key)
            ?: default
    }

    val buildVersion: String
        get() = getProperty("compass.build.version", "0.2.1")

    val coreVersion: String
        get() = getProperty("compass.core.version", buildVersion)

    val stateVersion: String
        get() = getProperty("compass.state.version", buildVersion)

    val componentStateVersion: String
        get() = getProperty("compass.component.state.version", buildVersion)

    val screenStateVersion: String
        get() = getProperty("compass.screen.state.version", buildVersion)

    val groupId: String
        get() = getProperty("compass.group.id", "com.usmonie.compass")
}

// Make versions available to all compass library projects
allprojects {
    // Set version for compass libraries
    version = compassVersions.buildVersion

    // Set specific group ID based on the library
    group = when (name) {
        "core" -> "${compassVersions.groupId}.core"
        "state" -> "${compassVersions.groupId}.state"
        "component-state" -> "${compassVersions.groupId}.component.state"
        "screen-state" -> "${compassVersions.groupId}.screen.state"
        else -> compassVersions.groupId
    }
}
