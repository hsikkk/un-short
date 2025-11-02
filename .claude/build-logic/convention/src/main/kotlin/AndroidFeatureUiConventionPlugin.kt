import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

@Suppress("UnstableApiUsage")
class AndroidFeatureUiConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply {
                apply("app.kotlin.android.library")
                apply("app.compose.library")
                apply("app.hilt")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                "implementation"(project(":core:domain"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:common"))
                "implementation"(project(":core:viewmodel"))

                "implementation"(libs.findLibrary("immutable").get())
                "implementation"(libs.findLibrary("lifecycle.runtime.ktx").get())
                "implementation"(libs.findLibrary("lifecycle.compose").get())
                "implementation"(libs.findLibrary("lifecycle.viewmodel.compose").get())

                "implementation"(libs.findLibrary("orbit.core").get())
                "implementation"(libs.findLibrary("orbit.viewmodel").get())
                "implementation"(libs.findLibrary("orbit.compose").get())

                "implementation"(libs.findLibrary("androidx.hilt.navigation.compose").get())
                "implementation"(libs.findLibrary("material.icons.extended").get())
            }
        }
    }
}
