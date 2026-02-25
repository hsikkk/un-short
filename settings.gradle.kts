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
