plugins {
    id("compass-compose")
    id("compass-publish")
}

// Publishing configuration moved to CompassPublishConventionPlugin

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.compose)
            implementation(libs.ui.backhandler)

            implementation(projects.componentState)
            implementation(projects.state)
        }
    }
}

// Configure signing for Maven Central
// signing {
//     val signingKey: String? by project
//     val signingPassword: String? by project
//     useInMemoryPgpKeys(signingKey, signingPassword)
//     sign(publishing.publications)
// }

// Only sign when publishing to Maven Central
// tasks.withType<Sign>().configureEach {
//     onlyIf { !gradle.taskGraph.hasTask(":publishToMavenLocal") }
// }
