import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CompassFeatureDomainConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply {
                apply("compass-base-kmp-conventions")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(versionCatalog.findLibrary("kotlinx-serialization-json").get())
                    }
                }
            }
        }
    }
}
