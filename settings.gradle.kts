rootProject.name = "compass"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic/convention")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

include(":compass:state")
project(":compass:state").projectDir = file("state")
include(":compass:component-state")
project(":compass:component-state").projectDir = file("component-state")
include(":compass:screen-state")
project(":compass:screen-state").projectDir = file("screen-state")
include(":compass:example")
project(":compass:example").projectDir = file("example")
