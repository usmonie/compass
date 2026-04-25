import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

class CompassExampleAppConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply {
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            setupCompassAndroidApplication()

            extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-Xwhen-guards",
                        "-Xskip-prerelease-check",
                        "-Xcontext-parameters",
                        "-Xexpect-actual-classes",
                        "-Xexplicit-backing-fields",
                        "-XXLanguage:+ExplicitBackingFields",
                    )
                }
            }

            dependencies.apply {
                add("implementation", versionCatalog.findLibrary("compose-runtime").get())
                add("implementation", versionCatalog.findLibrary("ui").get())
                add("implementation", versionCatalog.findLibrary("ui-tooling-preview").get())
                add("implementation", versionCatalog.findLibrary("compose-foundation").get())
                add("implementation", versionCatalog.findLibrary("androidx-material3").get())
                add("implementation", versionCatalog.findLibrary("components-resources").get())
                add("implementation", versionCatalog.findLibrary("coil-compose").get())
                add("implementation", versionCatalog.findLibrary("ui-backhandler").get())
                add("implementation", versionCatalog.findLibrary("kotlinx-serialization-json").get())
                add("implementation", versionCatalog.findLibrary("coroutines-core").get())
                add("implementation", versionCatalog.findLibrary("androidx-collections").get())
                add("implementation", versionCatalog.findLibrary("ui-tooling").get())
                add("implementation", versionCatalog.findLibrary("androidx-activity-compose").get())
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
