package com.hsikkk.delightroom

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    pluginManager.apply("org.jetbrains.kotlin.android")

    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

    val mSdkVersion = project.findProperty("app.minSdkVersion") as String
    val cSdkVersion = project.findProperty("app.compileSdkVersion") as String

    commonExtension.apply {
        compileSdk = cSdkVersion.toInt()

        defaultConfig {
            minSdk = mSdkVersion.toInt()
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
        }

        dependencies{
            "implementation"(libs.findLibrary("core.ktx").get())
        }
    }
}

fun CommonExtension<*, *, *, *, *, *>.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
