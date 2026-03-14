import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CompassBaseKmpConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
            }

            val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
            kotlin.apply {
                jvmToolchain(22)
                explicitApi()

                setupCompassAndroidLibrary()

                jvm()
                iosArm64()
                iosSimulatorArm64()

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
                    val commonMain = getByName("commonMain")
                    commonMain.dependencies {
                        implementation(versionCatalog.findLibrary("coroutines-core").get())
                        implementation(versionCatalog.findLibrary("androidx-collections").get())
                    }
                    val commonTest = getByName("commonTest")
                    commonTest.dependencies {
                        implementation(kotlin("test"))
                        implementation(versionCatalog.findLibrary("kotlinx-coroutines-test").get())
                    }
                }
            }
        }
    }
}
