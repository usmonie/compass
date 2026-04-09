plugins {
    `kotlin-dsl`
}

group = "com.usmonie.compass.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

kotlin {
    jvmToolchain(23)
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.compose.gradle.plugin)
    implementation(libs.compose.compiler.gradle.plugin)
    implementation(libs.vanniktech.publish.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation("org.jetbrains.kotlin:kotlin-serialization:${libs.versions.kotlin.get()}")
}

gradlePlugin {
    plugins {
        register("compassBaseKmpConventions") {
            id = "compass-base-kmp-conventions"
            implementationClass = "CompassBaseKmpConventionPlugin"
        }
        register("compassCompose") {
            id = "compass-compose"
            implementationClass = "CompassComposeConventionPlugin"
        }
        register("compassFeatureDomain") {
            id = "compass-feature-domain"
            implementationClass = "CompassFeatureDomainConventionPlugin"
        }
        register("compassPublish") {
            id = "compass-publish"
            implementationClass = "CompassPublishConventionPlugin"
        }
        register("compassExampleApp") {
            id = "compass-example-app"
            implementationClass = "CompassExampleAppConventionPlugin"
        }
    }
}
