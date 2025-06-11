import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
    // id("signing") // Will be enabled when signing is properly configured
}

// Configure signing for Maven Central (commented out for now)
// signing {
//     val signingKey: String? by project
//     val signingPassword: String? by project
//     useInMemoryPgpKeys(signingKey, signingPassword)
//     sign(publishing.publications)
// }

group = "com.usmonie.compass.state"
version = "0.2.0"

publishing {
    publications {
        withType<MavenPublication> {
            // POM configuration required by Maven Central
            pom {
                name.set("Compass State")
                description.set("MVI state management library for Kotlin Multiplatform with Jetpack Compose integration")
                url.set("https://github.com/usmonie/compass/")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("usmonie")
                        name.set("Compass Team")
                        email.set("compass@usmonie.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/usmonie/compass.git")
                    developerConnection.set("scm:git:ssh://github.com/usmonie/compass.git")
                    url.set("https://github.com/usmonie/compass")
                }
            }
        }
    }

    // Configure repository
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername") as String?
                password = System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword") as String?
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/usmonie/compass")
            credentials {
                username = System.getenv("GITHUB_USERNAME") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}

kotlin {
    jvmToolchain(23)
    androidTarget {
        publishLibraryVariants("release")
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    explicitApi()

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

    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
        languageSettings.enableLanguageFeature("-Xskip-prerelease-check")
    }

    // Add JVM target for desktop/server applications
    jvm()

    // Add JS target for web applications
    js(IR) {
        browser()
        nodejs()

        // Configure tests to use Node.js instead of browser
        browser {
            testTask {
                enabled = false // Disable browser tests
            }
        }

        // Use Node.js for testing
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }
    }

    // Add WASM target for future web support
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()

        // Disable browser tests for WASM as well
        browser {
            testTask {
                enabled = false // Disable browser tests that require Chrome
            }
        }
    }

    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
        languageSettings.enableLanguageFeature("-Xskip-prerelease-check")
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.collections)
            implementation(compose.runtime)
            implementation(compose.foundation)

            implementation(projects.compass.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
        }

            jvmMain.dependencies {
                implementation(compose.desktop.common)
            }

            jsMain.dependencies {
                implementation(compose.html.core)
            }
    }
}

android {
    namespace = "com.usmonie.compass.state"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
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
            packageName = "Compass State"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "com.usmonie.compass.state.desktopApp"
            }
        }
    }
}
