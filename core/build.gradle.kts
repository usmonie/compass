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

// Version and group are now set by compass-versions.gradle.kts

publishing {
    publications {
        withType<MavenPublication> {
            // POM configuration required by Maven Central
            pom {
                name.set("Compass Core")
                description.set("Type-safe navigation library for Kotlin Multiplatform with Jetpack Compose integration")
                url.set("https://github.com/usmonie/compass")

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
        // Maven Local repository for local development
        mavenLocal()

        // GitLab Maven repository (conditional and exclusive when enabled)
        if (project.hasProperty("enableGitlabPublish")) {
            val gitlabProjectId = project.findProperty("gitlabProjectId") as String?
                ?: System.getenv("GITLAB_PROJECT_ID")
            val gitlabToken = project.findProperty("gitlabToken") as String?
                ?: System.getenv("GITLAB_TOKEN")
            val gitlabUrl = project.findProperty("gitlabUrl") as String?
                ?: "https://gitlab.coinkeep.com/api/v4/projects/$gitlabProjectId/packages/maven"

            if (gitlabToken != null && gitlabProjectId != null) {
                maven {
                    name = "GitLab"
                    url = uri(gitlabUrl)
                    credentials(HttpHeaderCredentials::class) {
                        name = "Private-Token"
                        value = gitlabToken
                    }
                    authentication {
                        create("header", HttpHeaderAuthentication::class)
                    }
                }
            }
        }
        // GitHub Packages repository (conditional and exclusive when enabled)  
        else if (project.hasProperty("enableGithubPublish")) {
            val githubUsername = project.findProperty("githubUsername") as String?
                ?: System.getenv("GITHUB_USERNAME") ?: "usmonie"
            val githubRepository = project.findProperty("githubRepository") as String?
                ?: System.getenv("GITHUB_REPOSITORY") ?: "compass"
            val githubToken = project.findProperty("githubToken") as String?
                ?: System.getenv("GITHUB_TOKEN")

            if (githubToken != null) {
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/$githubUsername/$githubRepository")
                    credentials {
                        username = githubUsername
                        password = githubToken
                    }
                }
            }
        } else {
            // Default repositories only when not publishing to GitLab or GitHub
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username =
                        System.getenv("OSSRH_USERNAME") ?: findProperty("ossrhUsername") as String?
                    password =
                        System.getenv("OSSRH_PASSWORD") ?: findProperty("ossrhPassword") as String?
                }
            }
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/usmonie/compass")
                credentials {
                    username =
                        System.getenv("GITHUB_USERNAME") ?: findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
                }
            }
        }
    }
}

kotlin {
    explicitApi()
    jvmToolchain(23) // Changed from 17 to 23 to match installed Java version
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
            implementation(libs.kotlinx.serialization.core)
            api(libs.androidx.navigation3.compose)
            api(libs.ui.backhandler)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activity.compose)
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
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
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

dependencies {
  api("androidx.compose.runtime:runtime:1.10.0-alpha04")
  api("androidx.navigation3:navigation3-runtime:1.0.0-alpha10")
  api("org.jetbrains.kotlin:kotlin-stdlib:2.2.20")
}
