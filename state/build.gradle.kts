plugins {
    id("compass-feature-domain")
    id("compass-publish")
}

// Version and group are now set by compass-versions.gradle.kts
// Publishing configuration moved to CompassPublishConventionPlugin

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.compose)
        }
    }
}
