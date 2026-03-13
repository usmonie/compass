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
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.metro.gradle.plugin)
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
    }
}
