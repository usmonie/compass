import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CompassComposeConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply {
                apply("compass-feature-domain")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(versionCatalog.findLibrary("compose-runtime").get())
                        implementation(versionCatalog.findLibrary("ui").get())
                        implementation(versionCatalog.findLibrary("ui-tooling-preview").get())
                        implementation(versionCatalog.findLibrary("compose-foundation").get())
                        implementation(versionCatalog.findLibrary("androidx-material3").get())
                        implementation(versionCatalog.findLibrary("components-resources").get())
                        implementation(versionCatalog.findLibrary("coil-compose").get())
                    }

                    androidMain.dependencies {
                        implementation(versionCatalog.findLibrary("ui-tooling").get())
                    }

                    jvmMain.dependencies {
                        implementation(versionCatalog.findLibrary("desktop").get())
                    }
                }
            }

            extensions.configure<ComposeExtension> {
                configure<ResourcesExtension> {
                    publicResClass = false
                    generateResClass = never
                }
            }
        }
    }
}
