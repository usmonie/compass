import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

// Version and group are now set by compass-versions.gradle.kts

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Compass Component State")
                description.set("Compose UI components for MVI state management library")
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

    repositories {
        mavenLocal()

        // GitLab Maven repository (conditional)
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
        // GitHub Packages repository (conditional)
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
        }
    }
}

kotlin {
    jvmToolchain(23)
    androidTarget {
        publishLibraryVariants("release")
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
            baseName = "CompassComponentState"
            isStatic = true
        }
    }

    // Desktop JVM target
    jvm()

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
            api(projects.compass.core)
            api(projects.compass.state)
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
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
