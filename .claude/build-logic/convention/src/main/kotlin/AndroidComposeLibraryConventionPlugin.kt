import com.android.build.api.dsl.LibraryExtension
import com.hsikkk.delightroom.configureAndroidCompose
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidComposeLibraryConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            val extension = extensions.getByType<LibraryExtension>()
            configureAndroidCompose(extension)
        }
    }
}
