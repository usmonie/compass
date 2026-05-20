plugins {
    id("compass-example-app")
}

dependencies {
    implementation(projects.state)
    implementation(projects.componentState)
    implementation(projects.screenState)
    implementation(libs.androidx.navigation3.compose)

    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.android.test.ext.junit)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.compose.ui.test)
    debugImplementation(libs.compose.ui.test.manifest)
}
