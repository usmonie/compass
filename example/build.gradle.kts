plugins {
    id("compass-example-app")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.state)
            implementation(projects.componentState)
            implementation(projects.screenState)
            implementation(libs.androidx.navigation3.compose)
        }
    }
}
