import com.android.build.api.dsl.LibraryExtension
import {PACKAGE_NAME}.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidKotlinAndroidLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            val extension = extensions.getByType<LibraryExtension>()
            configureKotlinAndroid(extension)
        }
    }
}
