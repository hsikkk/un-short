plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.muuu.unshort"
    compileSdk = 35

    signingConfigs {
        create("release") {
            storeFile = file("/Users/muuu/key/key-store.jks")
            storePassword = "akffod02!!"
            keyAlias = "key0"
            keyPassword = "akffod02!!"
        }
    }

    defaultConfig {
        applicationId = "com.muuu.unshort"
        minSdk = 26
        targetSdk = 35
        versionCode = 15
        versionName = "1.2.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "AMPLITUDE_API_KEY", "\"cf5ac490fd69bf2bc05dff32b4c86938\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "AMPLITUDE_API_KEY", "\"c0d471f3a72a703e5b79055fad3b6191\"")

            // APK 최적화
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // Android 15+ compatibility
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Lifecycle Process (ProcessLifecycleOwner)
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // Amplitude Analytics
    implementation("com.amplitude:analytics-android:1.22.4")

    // Muuu Ad SDK
    implementation("com.muuu:ad:0.1.0")

    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Firebase BoM (Bill of Materials) - 버전 관리 간소화
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))

    // Firebase Analytics
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Firebase Remote Config
    implementation("com.google.firebase:firebase-config-ktx")

    // Affiliate 모듈
    implementation(project(":affiliate"))
}
