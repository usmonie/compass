import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.setupCompassAndroidLibrary() {
    androidLibrary {
        namespace = "com.usmonie.compass${project.group}.${project.name.replace("-", ".")}"
        compileSdk = 36
        minSdk = 24

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
        }
    }
}

fun Project.setupCompassAndroidApplication() {
    extensions.configure<ApplicationExtension> {
        namespace = "com.usmonie.compass.example"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.usmonie.compass.example"
            minSdk = 24
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_23
            targetCompatibility = JavaVersion.VERSION_23
        }
    }
}

internal val Project.versionCatalog
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
