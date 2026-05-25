pluginManagement {
    repositories {
        // No content filters here — plugins like KSP (com.google.devtools.*),
        // Hilt, Chaquopy need to resolve across multiple repos without restriction
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://chaquo.com/maven")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://chaquo.com/maven")
    }
}

rootProject.name = "Contrary Phone VPS"
include(":app")
