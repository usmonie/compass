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
        // languageSettings.enableLanguageFeature("-Xskip-prerelease-check") // Удаляем неподдерживаемую опцию
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            api(libs.androidx.collections)
            api(libs.coroutines.core)
            api(libs.androidx.navigation3.compose)
            implementation(projects.compass.core)
            implementation(projects.compass.state)
            implementation(projects.compass.componentState)
            implementation(projects.compass.screenState)
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
