plugins {
    id("compass-compose")
    id("compass-publish")
}

// Version and group are now set by compass-versions.gradle.kts
// Publishing configuration moved to CompassPublishConventionPlugin

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.state)
        }
    }
}
