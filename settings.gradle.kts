pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Muuu Ad SDK - GitHub Packages
        maven {
            url = uri("https://maven.pkg.github.com/hsikkk/ad")
            credentials {
                username = "hsikkk"
                password = "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"
            }
        }
    }
}

rootProject.name = "ShortBlock"
include(":app")
include(":affiliate")
