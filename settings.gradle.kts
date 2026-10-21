rootProject.name = "GuessWhoseSong"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    // Kotlin/Wasm's browser distribution task adds the pinned Node.js
    // distribution repository during setup. Allow that tool-managed repo.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":shared")
include(":server")

// Production server images only copy the modules required to build the Ktor
// service. Avoid configuring Android and Wasm projects that intentionally are
// not present in that Docker build context.
if (!System.getenv("SERVER_ONLY").equals("true", ignoreCase = true)) {
    include(":app")
    include(":web")
}
