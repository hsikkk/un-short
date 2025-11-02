import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.hsikkk.delightroom.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    compileOnly(libs.plugin.androidBuildTools)
    compileOnly(libs.plugin.kotlin)
}

gradlePlugin {
    plugins {
        register("composeLibrary") {
            id = "app.compose.library"
            implementationClass = "AndroidComposeLibraryConventionPlugin"
        }
        register("kotlinAndroidLibrary") {
            id = "app.kotlin.android.library"
            implementationClass = "AndroidKotlinAndroidLibraryConventionPlugin"
        }
        register("hilt") {
            id = "app.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("featureUI") {
            id = "app.feature.ui"
            implementationClass = "AndroidFeatureUiConventionPlugin"
        }
        register("featureNavigation") {
            id = "app.feature.navigation"
            implementationClass = "AndroidFeatureNavigationConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "app.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("app") {
            id = "app.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
