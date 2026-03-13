plugins {
    id("compass-compose")
}
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.compass.state)
            implementation(projects.compass.componentState)
            implementation(projects.compass.screenState)
            implementation(libs.androidx.navigation3.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

