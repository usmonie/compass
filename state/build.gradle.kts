plugins {
    id("feature-domain-conventions")
    id("maven-publish")
}

// Version and group are now set by compass-versions.gradle.kts

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Compass State")
                description.set("Pure MVI state management library for Kotlin Multiplatform - no UI dependencies")
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
    setupCompassAndroidLibrary()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.compose)
            implementation(libs.kotlinx.serialization.core)
        }
    }
}
