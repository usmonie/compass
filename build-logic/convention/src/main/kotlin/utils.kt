import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.setupCompassAndroidLibrary() {
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
        namespace = "com.usmonie.compass${project.group}.${project.name.replace("-", ".")}"
        compileSdk = 37
        minSdk = 24

        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)

        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-proguard-rules.pro")
            }
        }
    }
}

fun Project.setupCompassAndroidApplication() {
    extensions.configure<ApplicationExtension> {
        namespace = "com.usmonie.compass.example"
        compileSdk = 37

        defaultConfig {
            applicationId = "com.usmonie.compass.example"
            minSdk = 24
            targetSdk = 37
            versionCode = 1
            versionName = "1.0"
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = true
                proguardFiles(
                    "proguard-rules.pro"
                )
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
    }
}

internal val Project.versionCatalog
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
