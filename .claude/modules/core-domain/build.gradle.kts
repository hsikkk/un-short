plugins {
    id("app.kotlin.android.library")
}

android {
    namespace = "com.{{packageName}}.core.domain"
}

dependencies {
    implementation(libs.coroutine)
}