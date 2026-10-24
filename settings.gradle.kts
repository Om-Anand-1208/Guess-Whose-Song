rootProject.name = "GuessWhoseSong"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
include(":server")

// Production server images only copy the modules required to build the Ktor
// service. Avoid configuring the Android project in that Docker build context.
if (!System.getenv("SERVER_ONLY").equals("true", ignoreCase = true)) {
    include(":app")
}
