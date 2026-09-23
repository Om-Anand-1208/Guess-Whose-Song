val isServerOnly = System.getenv("SERVER_ONLY") == "true"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

if (!isServerOnly) {
    apply(plugin = "com.android.library")
}

kotlin {
    jvm() // for server
    
    if (!isServerOnly) {
        androidTarget() // for Android client
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

if (!isServerOnly) {
    configure<com.android.build.gradle.LibraryExtension> {
        namespace = "com.guesswhosesong.shared"
        compileSdk = 35
        defaultConfig {
            minSdk = 26
        }
    }
}
