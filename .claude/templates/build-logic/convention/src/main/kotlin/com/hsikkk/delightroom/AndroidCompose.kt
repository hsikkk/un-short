package com.hsikkk.delightroom

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
@Suppress("UnstableApiUsage")
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    commonExtension.apply {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        "implementation"(platform(libs.findLibrary("compose.bom").get()))
        "implementation"(libs.findLibrary("ui").get())
        "implementation"(libs.findLibrary("ui.graphics").get())
        "implementation"(libs.findLibrary("ui.tooling.preview").get())
        "implementation"(libs.findLibrary("material3").get())
        "debugImplementation"(libs.findLibrary("ui.tooling").get())
    }
}
