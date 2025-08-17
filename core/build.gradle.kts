import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    // id("signing") // Removed temporarily to fix linter errors, will configure signing later
}

// Version and group are now set by gradle/compass-versions.gradle.kts

// Publishing configuration is handled by gradle/compass-versions.gradle.kts

kotlin {
    jvmToolchain(23)
    androidTarget {
        publishLibraryVariants("release")
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    explicitApi()

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "CompassCore"
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

    // WASM target (экспериментальный - можно отключить через gradle.properties)
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
            implementation(libs.androidx.collections)

            implementation(libs.kotlinx.serialization.json)

            implementation(libs.androidx.navigationevent)
            implementation(libs.androidx.navigationevent.compose)

            api(libs.ui.backhandler)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.savedstate.ktx)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
        }

        // iOS dependencies
        iosMain.dependencies {
            // Add iOS-specific dependencies if needed
        }

        // Desktop (JVM) dependencies - связываем desktop target с jvmMain
        jvmMain.dependencies {
            implementation(compose.desktop.common)
        }

        // Web dependencies
        jsMain.dependencies {
            implementation(compose.html.core)
        }

        // WASM dependencies (только если включен WASM target)
        if (project.findProperty("compass.enable.wasm") == "true") {
            wasmJsMain.dependencies {
                // Пока не добавляем compose.html.core для WASM
                // implementation(compose.html.core) // Не поддерживается в текущей версии
            }
        }
    }
}

android {
    namespace = "com.usmonie.compass.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Compass Core"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "com.usmonie.compass.core.desktopApp"
            }
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
