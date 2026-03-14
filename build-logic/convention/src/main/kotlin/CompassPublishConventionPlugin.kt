import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class CompassPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")
            pluginManager.apply("org.jetbrains.dokka")

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
                signAllPublications()

                pom {
                    name.set(project.name)
                    description.set(when(project.name) {
                        "state" -> "Pure MVI state management library for Kotlin Multiplatform"
                        "component-state" -> "Compose UI components for MVI state management library"
                        "screen-state" -> "Type-safe navigation library for Kotlin Multiplatform with Jetpack Compose integration"
                        else -> "Compass Multiplatform Navigation & State Management"
                    })
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
                            name.set("Usman")
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
    }
}
