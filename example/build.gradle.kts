import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidApplication)
}
kotlin {
    explicitApi()
    jvmToolchain(23)
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "CompassState"
            isStatic = true
        }
    }

    // Desktop JVM target
    jvm()

    // Убираем проблемные native targets временно
    // Linux Native targets
    // linuxX64()
    // linuxArm64()

    // macOS Native targets
    // macosX64()
    // macosArm64()

    // Windows Native targets
    // mingwX64()

    // Web targets
    js(IR) {
        browser()
        nodejs()

        browser {
            testTask {
                enabled = false
            }
        }

        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }

    // WASM target (экспериментальный)
    if (project.findProperty("compass.enable.wasm") == "true") {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
        wasmJs {
            browser()

            browser {
                testTask {
                    enabled = false
                }
            }
        }
    }

    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(projects.compass.core)
            implementation(projects.compass.state)
            implementation(projects.compass.componentState)
            implementation(projects.compass.screenState)
            api(libs.androidx.navigation3.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.usmonie.compass.example"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }
}

dependencies {
  implementation("androidx.activity:activity-compose:1.12.0-alpha09")
  implementation("androidx.activity:activity:1.12.0-alpha09")
  implementation("androidx.compose.animation:animation-core:1.10.0-alpha04")
  implementation("androidx.compose.animation:animation:1.10.0-alpha04")
  implementation("androidx.compose.foundation:foundation-layout:1.10.0-alpha04")
  implementation("androidx.compose.foundation:foundation:1.10.0-alpha04")
  implementation("androidx.compose.material3:material3:1.5.0-alpha04")
  implementation("androidx.compose.runtime:runtime-saveable:1.10.0-alpha04")
  implementation("androidx.compose.runtime:runtime:1.10.0-alpha04")
  implementation("androidx.compose.ui:ui-graphics:1.10.0-alpha04")
  implementation("androidx.compose.ui:ui-text:1.10.0-alpha04")
  implementation("androidx.compose.ui:ui-unit:1.10.0-alpha04")
  implementation("androidx.compose.ui:ui:1.10.0-alpha04")
  implementation("androidx.navigation3:navigation3-runtime:1.0.0-alpha10")
  implementation("androidx.navigation3:navigation3-ui:1.0.0-alpha10")
  implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation(project(":compass:component-state"))
  implementation(project(":compass:core"))
  implementation(project(":compass:screen-state"))
  implementation(project(":compass:state"))
}
