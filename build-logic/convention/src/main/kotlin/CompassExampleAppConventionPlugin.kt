import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CompassExampleAppConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            pluginManager.apply {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.multiplatform")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                jvmToolchain(23)
                androidTarget()

                targets.all {
                    compilations.all {
                        compileTaskProvider.configure {
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
                    }
                }

                sourceSets.all {
                    languageSettings.enableLanguageFeature("ExplicitBackingFields")
                }

                sourceSets.apply {
                    commonMain.dependencies {
                        implementation(versionCatalog.findLibrary("compose-runtime").get())
                        implementation(versionCatalog.findLibrary("ui").get())
                        implementation(versionCatalog.findLibrary("ui-tooling-preview").get())
                        implementation(versionCatalog.findLibrary("compose-foundation").get())
                        implementation(versionCatalog.findLibrary("androidx-material3").get())
                        implementation(versionCatalog.findLibrary("components-resources").get())
                        implementation(versionCatalog.findLibrary("coil-compose").get())
                        implementation(versionCatalog.findLibrary("ui-backhandler").get())
                        implementation(versionCatalog.findLibrary("metro-runtime").get())
                        implementation(versionCatalog.findLibrary("kotlinx-serialization-json").get())
                        implementation(versionCatalog.findLibrary("kermit").get())
                        implementation(versionCatalog.findLibrary("coroutines-core").get())
                        implementation(versionCatalog.findLibrary("androidx-collections").get())
                    }

                    androidMain.dependencies {
                        implementation(versionCatalog.findLibrary("ui-tooling").get())
                        implementation(versionCatalog.findLibrary("androidx-activity-compose").get())
                    }
                }
            }

            setupCompassAndroidApplication()

            extensions.configure<ComposeExtension> {
                configure<ResourcesExtension> {
                    publicResClass = false
                    generateResClass = never
                }
            }
        }
    }
}
