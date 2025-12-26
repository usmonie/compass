plugins {
    id("compose")
    id("maven-publish")
    // id("signing") // Removed temporarily to fix linter errors, will configure signing later
}

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
    setupCompassAndroidLibrary()

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.navigation3.compose)
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
