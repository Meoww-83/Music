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
        exclusiveContent {
            forRepository {
                maven("https://jitpack.io")
            }
            filter {
                includeGroup("com.github.MetrolistGroup.innertubex")
                includeModule("com.github.MetrolistGroup", "innertubex")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Music"
include(":app")
include(":innertube")
