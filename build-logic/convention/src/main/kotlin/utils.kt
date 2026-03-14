import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
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

internal val Project.versionCatalog
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
