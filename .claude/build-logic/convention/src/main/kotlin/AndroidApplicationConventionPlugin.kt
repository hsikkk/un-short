import com.android.build.api.dsl.ApplicationExtension
import com.hsikkk.delightroom.configureAndroidCompose
import com.hsikkk.delightroom.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            val extension = extensions.getByType<ApplicationExtension>()
            configureKotlinAndroid(extension)
            configureAndroidCompose(extension)

            pluginManager.apply("app.hilt")

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

            dependencies {
                "implementation"(libs.findLibrary("compose.navigation").get())
                "implementation"(libs.findLibrary("activity.compose").get())
            }

        }
    }
}
