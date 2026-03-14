plugins {
    id("compass-compose")
}
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.state)
            implementation(projects.componentState)
            implementation(projects.screenState)
            implementation(libs.androidx.navigation3.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

